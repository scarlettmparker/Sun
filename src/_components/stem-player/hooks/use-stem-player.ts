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
  const gainNodes = useRef<GainNode[]>([]);
  const sources = useRef<AudioBufferSourceNode[]>([]);

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

  /**
   * Play the audio files.
   */
  const play = () => {
    if (!audioCtx || buffers.length === 0 || playing) return; // already playing, dgaf

    const now = audioCtx.currentTime;
    sources.current = buffers.map((buf, i) => {
      const src = audioCtx.createBufferSource();
      src.buffer = buf;
      src.connect(gainNodes.current[i]);
      gainNodes.current[i].connect(audioCtx.destination);

      // start that shi
      src.start(now);
      return src;
    });

    setPlaying(true);
  };

  /**
   * Stop playing.
   */
  const stop = () => {
    sources.current.forEach((src) => src.stop());
    sources.current = [];
    setPlaying(false);
  };

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
    play,
    stop,
    setVolume,
  };
}
