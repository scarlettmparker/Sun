import { useEffect, useRef, useState } from "react";
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
  const [position, setPosition] = useState<number>(0); // track where we are

  const gainNodes = useRef<GainNode[]>([]);
  const sources = useRef<AudioBufferSourceNode[]>([]);

  // want controls
  const startTime = useRef<number>(0);
  const offset = useRef<number>(0);
  const rafId = useRef<number | null>(null);

  useEffect(() => {
    // Can only be on the client
    if (typeof window === "undefined") return;
    setAudioCtx(new AudioContext());
  }, []);

  useEffect(() => {
    if (!audioCtx) return;
    let mounted = true;

    // Load & decode all the stems
    Promise.all(
      stems.map((stem) =>
        fetch(stem.url)
          .then((res) => res.arrayBuffer())
          .then((buf) => audioCtx.decodeAudioData(buf))
      )
    ).then((decoded) => {
      if (mounted) {
        setBuffers(decoded);
        gainNodes.current = decoded.map(() => audioCtx.createGain());
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
      gainNodes.current[i].connect(audioCtx.destination);

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
        setPosition(audioCtx.currentTime - startTime.current);
        rafId.current = requestAnimationFrame(update);
      }
    };

    rafId.current = requestAnimationFrame(update);
  };

  /**
   * Play the audio files.
   */
  const play = () => {
    if (!audioCtx || buffers.length === 0 || playing) return;
    startPlayback(offset.current);
  };

  /**
   * Stop playing.
   */
  const stop = () => {
    sources.current.forEach((src) => {
      try {
        src.stop();
      } catch (_) {} // don't care
    });
    sources.current = [];

    // keep offset
    if (audioCtx && playing) {
      offset.current = Math.max(0, audioCtx.currentTime - startTime.current);
      setPosition(offset.current);
    }

    setPlaying(false);
  };

  /**
   * Seek to a specific time (in seconds).
   *
   * @param time Second to seek to.
   */
  const seek = (time: number) => {
    offset.current = Math.max(0, Math.min(getDuration(), time));
    setPosition(offset.current); // update position immediately
    if (playing) startPlayback(offset.current);
  };

  /**
   * Skip forward/backward by a given no seconds.
   */
  const skip = (seconds: number) => {
    const newTime = Math.max(0, Math.min(getDuration(), position + seconds));
    seek(newTime);
  };

  /**
   * Get total duration (all stems must therefore be same length).
   * TODO: get longest? Dunno.
   */
  const getDuration = () => buffers[0]?.duration || 0;

  /**
   * Set volume.
   *
   * @param index Audio to set volume of.
   * @param value Value to set audio volume to.
   */
  const setVolume = (index: number, value: number) => {
    const node = gainNodes.current[index];
    if (node) node.gain.setValueAtTime(value, audioCtx!.currentTime);
  };

  return {
    loaded: buffers.length === stems.length,
    playing,
    position,
    duration: getDuration(),
    play,
    stop,
    seek,
    skip,
    setVolume,
  };
}
