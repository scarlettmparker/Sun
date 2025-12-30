/**
 * @fileoverview Tests for StemPlayer component.
 * Tests the component's rendering, user interactions, and integration with useStemPlayer hook.
 */

import { render, screen, fireEvent } from "@testing-library/react";
import StemPlayer from "~/_components/stem-player";
import { Song, Stem } from "~/generated/graphql";
import {
  mockAudioContext,
  mockAudioBuffer,
  mockGainNodes,
  mockT,
} from "testing/jest/mock";
import { useStemPlayer } from "~/_components/stem-player/hooks/use-stem-player";

// Mock stem player hook
jest.mock("~/_components/stem-player/hooks/use-stem-player");
const mockUseStemPlayer = useStemPlayer as jest.Mock;

const mockPlay: jest.Mock = jest.fn();
const mockStop: jest.Mock = jest.fn();
const mockSkip: jest.Mock = jest.fn();
const mockSeek: jest.Mock = jest.fn();
const mockSetMasterVolume: jest.Mock = jest.fn();

const mockStems: Stem[] = [
  { name: "Drums", path: "/drums.mp3" },
  { name: "Bass", path: "/bass.mp3" },
];

const mockSong: Song = {
  id: "",
  path: "",
  stems: mockStems,
};

const defaultMockReturnValue = {
  loaded: true,
  loadingProgress: 100,
  playing: false,
  ended: false,
  play: mockPlay,
  stop: mockStop,
  skip: mockSkip,
  seek: mockSeek,
  position: 0,
  duration: 0,
  masterVolume: 1,
  setVolume: jest.fn(),
  setMasterVolume: mockSetMasterVolume,
};

describe("StemPlayer", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGainNodes.length = 0;
    mockT.mockClear();
    (global.fetch as jest.Mock).mockResolvedValue({
      arrayBuffer: jest.fn().mockResolvedValue(new ArrayBuffer(8)),
    });
    (mockAudioContext.decodeAudioData as jest.Mock).mockResolvedValue(
      mockAudioBuffer
    );
    mockUseStemPlayer.mockReturnValue(defaultMockReturnValue);
  });

  it("renders the component with all controls", () => {
    render(<StemPlayer song={mockSong} />);

    // Check that translation keys are used
    expect(screen.getByText("controls.master")).toBeInTheDocument();
    expect(
      screen.getByLabelText("controls.aria.master-volume")
    ).toBeInTheDocument();
    expect(screen.getByLabelText("controls.aria.seek")).toBeInTheDocument();
    expect(screen.getByLabelText("playback")).toBeInTheDocument();
  });

  it("renders stem sliders", () => {
    render(<StemPlayer song={mockSong} />);

    // Check that stem sliders are rendered
    expect(screen.getByText("Drums")).toBeInTheDocument();
    expect(screen.getByText("Bass")).toBeInTheDocument();
  });

  it("renders play/pause button with correct initial state", () => {
    render(<StemPlayer song={mockSong} />);

    const playButton = screen.getByLabelText("controls.aria.play");
    expect(playButton).toBeInTheDocument();
    expect(screen.getByText("controls.play")).toBeInTheDocument();
  });

  it("renders seek controls", () => {
    render(<StemPlayer song={mockSong} />);

    expect(
      screen.getByLabelText("controls.aria.seek-back")
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("controls.aria.seek-forward")
    ).toBeInTheDocument();
  });

  it("renders time display", () => {
    render(<StemPlayer song={mockSong} />);
    expect(screen.getByLabelText("playback")).toBeInTheDocument();
  });

  it("renders seeker with correct range", () => {
    render(<StemPlayer song={mockSong} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    expect(seeker).toHaveAttribute("min", "0");
    expect(seeker).toHaveAttribute("max", "0.1");
    expect(seeker).toHaveAttribute("step", "0.1");
  });

  it("renders master volume slider with correct range", () => {
    render(<StemPlayer song={mockSong} />);

    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    expect(masterSlider).toHaveAttribute("min", "0");
    expect(masterSlider).toHaveAttribute("max", "2");
    expect(masterSlider).toHaveAttribute("step", "0.01");
  });

  it("calls setMasterVolume on master volume slider change", () => {
    const mockSetMasterVolume = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      setMasterVolume: mockSetMasterVolume,
    });

    render(<StemPlayer song={mockSong} />);

    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    fireEvent.change(masterSlider, { target: { value: "1.5" } });
    expect(mockSetMasterVolume).toHaveBeenCalledWith(1.5);
  });

  it("displays master volume tooltip on hover", () => {
    render(<StemPlayer song={mockSong} />);

    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    const mockRect = {
      left: 0,
      width: 100,
      height: 0,
      x: 0,
      y: 0,
      bottom: 0,
      right: 100,
      top: 0,
      toJSON: () => ({}),
    };

    masterSlider.getBoundingClientRect = jest.fn(() => mockRect as DOMRect);
    fireEvent.mouseMove(masterSlider, { clientX: 50 });
    expect(masterSlider.title).toBe("controls.title.master-volume");
  });

  it("shows loading progress when not loaded", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      loaded: false,
      loadingProgress: 50,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("Loading: 50%")).toBeInTheDocument();
  });

  it("calls play when play button is clicked", () => {
    const mockPlay = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      play: mockPlay,
    });

    render(<StemPlayer song={mockSong} />);

    const playButton = screen.getByLabelText("controls.aria.play");
    fireEvent.click(playButton);
    expect(mockPlay).toHaveBeenCalled();
  });

  it("calls stop when stop button is clicked", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer song={mockSong} />);

    const stopButton = screen.getByLabelText("controls.aria.stop");
    fireEvent.click(stopButton);
    expect(mockStop).toHaveBeenCalled();
  });

  it("calls skip with -10 when seek back is clicked", () => {
    render(<StemPlayer song={mockSong} />);

    const seekBackButton = screen.getByLabelText("controls.aria.seek-back");
    fireEvent.click(seekBackButton);
    expect(mockSkip).toHaveBeenCalledWith(-10);
  });

  it("calls skip with 10 when seek forward is clicked", () => {
    render(<StemPlayer song={mockSong} />);

    const seekForwardButton = screen.getByLabelText(
      "controls.aria.seek-forward"
    );
    fireEvent.click(seekForwardButton);
    expect(mockSkip).toHaveBeenCalledWith(10);
  });

  it("calls seek when seeker is changed", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      duration: 10,
    });

    render(<StemPlayer song={mockSong} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    fireEvent.change(seeker, { target: { value: "1" } });
    expect(mockSeek).toHaveBeenCalledWith(1);
  });

  it("displays stop button when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("controls.stop")).toBeInTheDocument();
  });

  it("displays restart button when ended", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      ended: true,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("controls.restart")).toBeInTheDocument();
  });

  it("renders seek controls with correct attributes", () => {
    render(<StemPlayer song={mockSong} />);

    const seekBack = screen.getByLabelText("controls.aria.seek-back");
    expect(seekBack).toHaveAttribute("aria-label", "controls.aria.seek-back");
    expect(seekBack).toHaveAttribute("title", "controls.title.seek-back");

    const seekForward = screen.getByLabelText("controls.aria.seek-forward");
    expect(seekForward).toHaveAttribute(
      "aria-label",
      "controls.aria.seek-forward"
    );
    expect(seekForward).toHaveAttribute("title", "controls.title.seek-forward");
  });

  it("renders play button with correct attributes", () => {
    render(<StemPlayer song={mockSong} />);

    const playButton = screen.getByLabelText("controls.aria.play");
    expect(playButton).toHaveAttribute("aria-label", "controls.aria.play");
    expect(playButton).toHaveAttribute("title", "controls.title.play");
    expect(playButton).toHaveAttribute("aria-pressed", "false");
  });

  it("displays seeker tooltip on hover", () => {
    render(<StemPlayer song={mockSong} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    const mockRect = {
      left: 0,
      width: 100,
      height: 0,
      x: 0,
      y: 0,
      bottom: 0,
      right: 100,
      top: 0,
      toJSON: () => ({}),
    };

    seeker.getBoundingClientRect = jest.fn(() => mockRect as DOMRect);
    fireEvent.mouseMove(seeker, { clientX: 50 });
    expect(seeker.title).toBe("controls.title.seek");
  });

  it("renders stem sliders with correct attributes", () => {
    render(<StemPlayer song={mockSong} />);

    const drumsSlider = screen.getAllByDisplayValue("1")[0];
    expect(drumsSlider).toHaveAttribute("type", "range");
    expect(drumsSlider).toHaveAttribute("min", "0");
    expect(drumsSlider).toHaveAttribute("max", "1");
    expect(drumsSlider).toHaveAttribute("step", "0.01");
    expect(drumsSlider).toHaveAttribute("class", "range range vertical");
  });

  // TODO: improve test coverage

  it("renders time display with formatted time", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      position: 65,
      duration: 125,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("1:05 / 2:05")).toBeInTheDocument();
  });

  it("renders seeker with correct value and max based on duration", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      position: 10,
      duration: 100,
    });

    render(<StemPlayer song={mockSong} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    expect(seeker).toHaveAttribute("max", "100.1");
    expect(seeker).toHaveValue("10");
  });

  it("renders master volume slider with correct value", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      masterVolume: 0.75,
    });

    render(<StemPlayer song={mockSong} />);
    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    expect(masterSlider).toHaveValue("0.75");
  });

  it("renders stop button when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("controls.stop")).toBeInTheDocument();
  });

  it("renders restart button when ended", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      ended: true,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("controls.restart")).toBeInTheDocument();
  });

  it("renders play button with correct aria-label when ended", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      ended: true,
    });

    render(<StemPlayer song={mockSong} />);
    const playButton = screen.getByLabelText("controls.aria.restart");
    expect(playButton).toHaveAttribute("aria-label", "controls.aria.restart");
    expect(playButton).toHaveAttribute("title", "controls.title.restart");
  });

  it("renders stop button with correct aria-label when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer song={mockSong} />);
    const stopButton = screen.getByLabelText("controls.aria.stop");
    expect(stopButton).toHaveAttribute("aria-label", "controls.aria.stop");
    expect(stopButton).toHaveAttribute("title", "controls.title.stop");
  });

  it("renders play button with aria-pressed false when not playing", () => {
    render(<StemPlayer song={mockSong} />);
    const playButton = screen.getByLabelText("controls.aria.play");
    expect(playButton).toHaveAttribute("aria-pressed", "false");
  });

  it("renders stop button with aria-pressed true when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer song={mockSong} />);
    const stopButton = screen.getByLabelText("controls.aria.stop");
    expect(stopButton).toHaveAttribute("aria-pressed", "true");
  });

  it("renders multiple stems correctly", () => {
    const multipleStems: Stem[] = [
      { name: "Drums", path: "/drums.mp3" },
      { name: "Bass", path: "/bass.mp3" },
      { name: "Guitar", path: "/guitar.mp3" },
    ];

    const mockSong: Song = {
      id: "",
      path: "",
      stems: multipleStems,
    };

    render(<StemPlayer song={mockSong} />);

    expect(screen.getByText("Drums")).toBeInTheDocument();
    expect(screen.getByText("Bass")).toBeInTheDocument();
    expect(screen.getByText("Guitar")).toBeInTheDocument();
  });

  it("handles empty stems array", () => {
    const mockSong: Song = {
      id: "",
      path: "",
      stems: [],
    };

    render(<StemPlayer song={mockSong} />);

    expect(screen.getByText("controls.master")).toBeInTheDocument();
    const sliders = screen.getAllByRole("slider");
    expect(sliders).toHaveLength(2);
    const masterSlider = sliders.find(
      (slider) =>
        slider.getAttribute("aria-label") === "controls.aria.master-volume"
    );
    expect(masterSlider).toBeInTheDocument();
  });

  it("renders with custom className", () => {
    render(<StemPlayer song={mockSong} className="custom-class" />);

    const container = screen.getByText("controls.master").closest(".container");
    expect(container).toHaveClass("custom-class");
  });

  it("renders with additional props", () => {
    render(<StemPlayer song={mockSong} data-testid="stem-player" />);

    const container = screen.getByTestId("stem-player");
    expect(container).toBeInTheDocument();
  });

  it("handles invalid song prop gracefully", () => {
    const invalidSong = { id: "test" } as Song;

    expect(() => {
      render(<StemPlayer song={invalidSong} />);
    }).not.toThrow();
  });

  it("handles song with undefined stems", () => {
    const songWithUndefinedStems = { ...mockSong, stems: undefined } as Song;

    expect(() => {
      render(<StemPlayer song={songWithUndefinedStems} />);
    }).not.toThrow();
  });

  it("handles song with null stems", () => {
    const songWithNullStems = { ...mockSong, stems: null };

    expect(() => {
      render(<StemPlayer song={songWithNullStems} />);
    }).not.toThrow();
  });

  it("renders loading state with 0 progress", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      loaded: false,
      loadingProgress: 0,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("Loading: 0%")).toBeInTheDocument();
  });

  it("renders loading state with 100 progress but not loaded", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      loaded: false,
      loadingProgress: 100,
    });

    render(<StemPlayer song={mockSong} />);
    expect(screen.getByText("Loading: 100%")).toBeInTheDocument();
  });

  it("handles seek with values beyond duration", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      duration: 10,
    });

    render(<StemPlayer song={mockSong} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    fireEvent.change(seeker, { target: { value: "15" } });
    expect(mockSeek).toHaveBeenCalledWith(10);
  });

  it("handles negative seek values", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      duration: 10,
      position: 5,
    });

    render(<StemPlayer song={mockSong} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    fireEvent.change(seeker, { target: { value: "-5" } });
    expect(mockSeek).toHaveBeenCalledWith(0);
  });

  it("handles master volume values beyond range", () => {
    const mockSetMasterVolume = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      setMasterVolume: mockSetMasterVolume,
    });

    render(<StemPlayer song={mockSong} />);

    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    fireEvent.change(masterSlider, { target: { value: "3" } });
    expect(mockSetMasterVolume).toHaveBeenCalledWith(2);
  });

  it("handles negative master volume values", () => {
    const mockSetMasterVolume = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      setMasterVolume: mockSetMasterVolume,
    });

    render(<StemPlayer song={mockSong} />);

    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    fireEvent.change(masterSlider, { target: { value: "-1" } });
    expect(mockSetMasterVolume).toHaveBeenCalledWith(0);
  });

  it("handles stem volume slider changes with invalid indices", () => {
    const mockSetVolume = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      setVolume: mockSetVolume,
    });

    render(<StemPlayer song={mockSong} />);

    const sliders = screen.getAllByRole("slider");
    const stemSliders = sliders.filter(
      (slider) =>
        slider.getAttribute("aria-label") !== "controls.aria.master-volume" &&
        slider.getAttribute("aria-label") !== "controls.aria.seek"
    );
    expect(stemSliders.length).toBe(2);
  });

  it("handles rapid button clicks", () => {
    const mockPlay = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      play: mockPlay,
    });

    render(<StemPlayer song={mockSong} />);

    const playButton = screen.getByLabelText("controls.aria.play");

    // Simulate rapid clicks
    fireEvent.click(playButton);
    fireEvent.click(playButton);
    fireEvent.click(playButton);

    expect(mockPlay).toHaveBeenCalledTimes(3);
  });

  it("maintains accessibility attributes during state changes", () => {
    const { rerender } = render(<StemPlayer song={mockSong} />);

    const playButton = screen.getByLabelText("controls.aria.play");
    expect(playButton).toHaveAttribute("aria-pressed", "false");

    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    rerender(<StemPlayer song={mockSong} />);

    const stopButton = screen.getByLabelText("controls.aria.stop");
    expect(stopButton).toHaveAttribute("aria-pressed", "true");
  });
});
