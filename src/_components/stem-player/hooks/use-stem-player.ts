import { useEffect, useRef, useState, useCallback, useMemo } from "react";
import { Stem } from "~/_components/stem-player/types/stem";

/**
 * Stem player hook using the Web Audio API.
 *
 * @param stem List of stems.
 */
export function useStemPlayer(stems: Stem[]) {
  const [audioCtx, setAudioCtx] = useState<AudioContext | null>(null);
  const [buffers, setBuffers] = useState<AudioBuffer[]>([]);
  const [playing, setPlaying] = useState<boolean>(false);
  const [ended, setEnded] = useState<boolean>(false);
  const [position, setPosition] = useState<number>(0); // track where we are
  const [loadingProgress, setLoadingProgress] = useState<number>(0);

  const gainNodes = useRef<GainNode[]>([]);
  const masterGainNode = useRef<GainNode | null>(null);
  const sources = useRef<AudioBufferSourceNode[]>([]);

  // want controls
  const startTime = useRef<number>(0);
  const offset = useRef<number>(0);
  const rafId = useRef<number | null>(null);
  const [masterVolume, setMasterVolumeState] = useState<number>(1);

  useEffect(() => {
    // Can only be on the client
    if (typeof window === "undefined") return;
    setAudioCtx(new AudioContext());
  }, []);

  useEffect(() => {
    if (!audioCtx) return;
    let mounted = true;
    setLoadingProgress(0);

    // Load & decode all the stems
    const loadPromises = stems.map((stem) =>
      fetch(stem.url)
        .then((res) => res.arrayBuffer())
        .then((buf) => audioCtx.decodeAudioData(buf))
        .then((decoded) => {
          if (mounted) {
            setLoadingProgress((prev) => prev + (1 / stems.length) * 100);
          }
          return decoded;
        })
    );

    Promise.all(loadPromises)
      .then((decoded) => {
        if (mounted) {
          setBuffers(decoded);
          gainNodes.current = decoded.map(() => audioCtx.createGain());
          masterGainNode.current = audioCtx.createGain();
          masterGainNode.current.gain.setValueAtTime(
            masterVolume,
            audioCtx.currentTime
          );
          gainNodes.current.forEach((gainNode) => {
            gainNode.connect(masterGainNode.current!);
          });
          masterGainNode.current.connect(audioCtx.destination);
        }
      })
      .catch((_) => {
        if (mounted) {
          // console.error("Failed to load one or more stems:", err);
        }
      });

    // cleanup
    return () => {
      mounted = false;
    };
  }, [stems, audioCtx]);

  // Start/stop position loop based on playing state
  useEffect(() => {
    if (playing) {
      loopPosition();
    } else {
      if (rafId.current) {
        cancelAnimationFrame(rafId.current);
        rafId.current = null;
      }
    }
  }, [playing]);

  /**
   * Start playback from specific offset.
   */
  const startPlayback = (startOffset = 0) => {
    if (!audioCtx || buffers.length === 0) return;

    stop();

    const now = audioCtx.currentTime;
    startTime.current = now - startOffset;
    offset.current = startOffset;
    setPosition(startOffset); // initialize position

    sources.current = buffers.map((buf, i) => {
      const src = audioCtx.createBufferSource();
      src.buffer = buf;
      src.connect(gainNodes.current[i]);
      gainNodes.current[i].connect(masterGainNode.current!);

      // start that shi
      src.start(now, startOffset);
      return src;
    });

    setPlaying(true);
  };

  /**
   * Continuously update position while playing.
   */
  const loopPosition = () => {
    if (!audioCtx) return;

    const update = () => {
      if (playing && audioCtx) {
        const currentPos = audioCtx.currentTime - startTime.current;
        setPosition(currentPos);
        if (currentPos >= getDuration()) {
          stop();
          setEnded(true);
        } else {
          rafId.current = requestAnimationFrame(update);
        }
      }
    };

    rafId.current = requestAnimationFrame(update);
  };

  /**
   * Play the audio files.
   */
  const play = useCallback(() => {
    if (!audioCtx || buffers.length === 0 || playing) return;
    if (ended) {
      offset.current = 0;
      setEnded(false);
    }
    startPlayback(offset.current);
  }, [audioCtx, buffers, playing, ended]);

  /**
   * Stop playing.
   */
  const stop = useCallback(() => {
    sources.current.forEach((src) => {
      try {
        src.stop();
      } catch (_) {
        // don't care
      }
    });
    sources.current = [];

    // keep offset
    if (audioCtx && playing) {
      offset.current = Math.max(0, audioCtx.currentTime - startTime.current);
      setPosition(offset.current);
    }

    setPlaying(false);
  }, [audioCtx, playing]);

  /**
   * Get total duration (all stems must therefore be same length).
   * TODO: get longest? Dunno.
   */
  const getDuration = useCallback(() => buffers[0]?.duration || 0, [buffers]);

  /**
   * Seek to a specific time (in seconds).
   *
   * @param time Second to seek to.
   */
  const seek = useCallback(
    (time: number) => {
      offset.current = Math.max(0, Math.min(getDuration(), time));
      setPosition(offset.current); // update position immediately
      if (offset.current >= getDuration()) {
        // End of the song
        setEnded(true);
        if (playing) stop();
      } else {
        setEnded(false);
      }
      if (playing && offset.current < getDuration())
        startPlayback(offset.current);
    },
    [audioCtx, buffers, playing, getDuration, stop]
  );

  /**
   * Skip forward/backward by a given no seconds.
   */
  const skip = useCallback(
    (seconds: number) => {
      const newTime = Math.max(0, Math.min(getDuration(), position + seconds));
      seek(newTime);
    },
    [audioCtx, buffers, position, seek]
  );

  /**
   * Set volume.
   *
   * @param index Audio to set volume of.
   * @param value Value to set audio volume to.
   */
  const setVolume = useCallback(
    (index: number, value: number) => {
      const node = gainNodes.current[index];
      if (node) node.gain.setValueAtTime(value, audioCtx!.currentTime);
    },
    [audioCtx]
  );

  /**
   * Set master volume.
   *
   * @param value Value to set master volume to.
   */
  const setMasterVolume = useCallback(
    (value: number) => {
      setMasterVolumeState(value);
      if (masterGainNode.current && audioCtx) {
        masterGainNode.current.gain.setValueAtTime(value, audioCtx.currentTime);
      }
    },
    [audioCtx]
  );

  // memoize the return value to prevent unnecessary re-renders
  return useMemo(
    () => ({
      loaded: buffers.length === stems.length,
      loadingProgress,
      playing,
      ended,
      position,
      duration: getDuration(),
      masterVolume,
      play,
      stop,
      seek,
      skip,
      setVolume,
      setMasterVolume,
    }),
    [
      buffers,
      stems,
      loadingProgress,
      playing,
      ended,
      position,
      getDuration,
      masterVolume,
      play,
      stop,
      seek,
      skip,
      setVolume,
      setMasterVolume,
    ]
  );
}
