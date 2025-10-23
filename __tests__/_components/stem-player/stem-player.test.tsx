/**
 * @fileoverview Tests for StemPlayer component.
 * Tests the component's rendering, user interactions, and integration with useStemPlayer hook.
 */

import { render, screen, fireEvent } from "@testing-library/react";
import StemPlayer from "~/_components/stem-player";
import { Stem } from "~/_components/stem-player/types/stem";
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
  { name: "Drums", url: "/drums.mp3" },
  { name: "Bass", url: "/bass.mp3" },
];

const defaultMockReturnValue = {
  loaded: true,
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
    render(<StemPlayer stems={mockStems} />);

    // Check that translation keys are used
    expect(screen.getByText("controls.master")).toBeInTheDocument();
    expect(
      screen.getByLabelText("controls.aria.master-volume")
    ).toBeInTheDocument();
    expect(screen.getByLabelText("controls.aria.seek")).toBeInTheDocument();
    expect(screen.getByLabelText("playback")).toBeInTheDocument();
  });

  it("renders stem sliders", () => {
    render(<StemPlayer stems={mockStems} />);

    // Check that stem sliders are rendered
    expect(screen.getByText("Drums")).toBeInTheDocument();
    expect(screen.getByText("Bass")).toBeInTheDocument();
  });

  it("renders play/pause button with correct initial state", () => {
    render(<StemPlayer stems={mockStems} />);

    const playButton = screen.getByLabelText("controls.aria.play");
    expect(playButton).toBeInTheDocument();
    expect(screen.getByText("controls.play")).toBeInTheDocument();
  });

  it("renders seek controls", () => {
    render(<StemPlayer stems={mockStems} />);

    expect(
      screen.getByLabelText("controls.aria.seek-back")
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("controls.aria.seek-forward")
    ).toBeInTheDocument();
  });

  it("renders time display", () => {
    render(<StemPlayer stems={mockStems} />);
    expect(screen.getByLabelText("playback")).toBeInTheDocument();
  });

  it("renders seeker with correct range", () => {
    render(<StemPlayer stems={mockStems} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    expect(seeker).toHaveAttribute("min", "0");
    expect(seeker).toHaveAttribute("max", "0.1");
    expect(seeker).toHaveAttribute("step", "0.1");
  });

  it("renders master volume slider with correct range", () => {
    render(<StemPlayer stems={mockStems} />);

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

    render(<StemPlayer stems={mockStems} />);

    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    fireEvent.change(masterSlider, { target: { value: "1.5" } });
    expect(mockSetMasterVolume).toHaveBeenCalledWith(1.5);
  });

  it("displays master volume tooltip on hover", () => {
    render(<StemPlayer stems={mockStems} />);

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

  it("shows fallback when not loaded", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      loaded: false,
    });

    const { container } = render(<StemPlayer stems={mockStems} />);
    expect(container.firstChild).toBeNull();
  });

  it("calls play when play button is clicked", () => {
    const mockPlay = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      play: mockPlay,
    });

    render(<StemPlayer stems={mockStems} />);

    const playButton = screen.getByLabelText("controls.aria.play");
    fireEvent.click(playButton);
    expect(mockPlay).toHaveBeenCalled();
  });

  it("calls stop when stop button is clicked", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer stems={mockStems} />);

    const stopButton = screen.getByLabelText("controls.aria.stop");
    fireEvent.click(stopButton);
    expect(mockStop).toHaveBeenCalled();
  });

  it("calls skip with -10 when seek back is clicked", () => {
    render(<StemPlayer stems={mockStems} />);

    const seekBackButton = screen.getByLabelText("controls.aria.seek-back");
    fireEvent.click(seekBackButton);
    expect(mockSkip).toHaveBeenCalledWith(-10);
  });

  it("calls skip with 10 when seek forward is clicked", () => {
    render(<StemPlayer stems={mockStems} />);

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

    render(<StemPlayer stems={mockStems} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    fireEvent.change(seeker, { target: { value: "1" } });
    expect(mockSeek).toHaveBeenCalledWith(1);
  });

  it("displays stop button when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer stems={mockStems} />);
    expect(screen.getByText("controls.stop")).toBeInTheDocument();
  });

  it("displays restart button when ended", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      ended: true,
    });

    render(<StemPlayer stems={mockStems} />);
    expect(screen.getByText("controls.restart")).toBeInTheDocument();
  });

  it("renders seek controls with correct attributes", () => {
    render(<StemPlayer stems={mockStems} />);

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
    render(<StemPlayer stems={mockStems} />);

    const playButton = screen.getByLabelText("controls.aria.play");
    expect(playButton).toHaveAttribute("aria-label", "controls.aria.play");
    expect(playButton).toHaveAttribute("title", "controls.title.play");
    expect(playButton).toHaveAttribute("aria-pressed", "false");
  });

  it("displays seeker tooltip on hover", () => {
    render(<StemPlayer stems={mockStems} />);

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
    render(<StemPlayer stems={mockStems} />);

    const drumsSlider = screen.getAllByDisplayValue("1")[0];
    expect(drumsSlider).toHaveAttribute("type", "range");
    expect(drumsSlider).toHaveAttribute("min", "0");
    expect(drumsSlider).toHaveAttribute("max", "1");
    expect(drumsSlider).toHaveAttribute("step", "0.01");
    expect(drumsSlider).toHaveAttribute("class", " range vertical");
  });

  it("calls setVolume when stem slider changes", () => {
    const mockSetVolume = jest.fn();
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      setVolume: mockSetVolume,
    });

    render(<StemPlayer stems={mockStems} />);

    const drumsSlider = screen.getAllByDisplayValue("1")[0];
    fireEvent.change(drumsSlider, { target: { value: "0.5" } });
    expect(mockSetVolume).toHaveBeenCalledWith(0, 0.5);
  });

  it("renders time display with formatted time", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      position: 65,
      duration: 125,
    });

    render(<StemPlayer stems={mockStems} />);
    expect(screen.getByText("1:05 / 2:05")).toBeInTheDocument();
  });

  it("renders seeker with correct value and max based on duration", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      position: 10,
      duration: 100,
    });

    render(<StemPlayer stems={mockStems} />);

    const seeker = screen.getByLabelText("controls.aria.seek");
    expect(seeker).toHaveAttribute("max", "100.1");
    expect(seeker).toHaveValue("10");
  });

  it("renders master volume slider with correct value", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      masterVolume: 0.75,
    });

    render(<StemPlayer stems={mockStems} />);
    const masterSlider = screen.getByLabelText("controls.aria.master-volume");
    expect(masterSlider).toHaveValue("0.75");
  });

  it("renders stop button when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer stems={mockStems} />);
    expect(screen.getByText("controls.stop")).toBeInTheDocument();
  });

  it("renders restart button when ended", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      ended: true,
    });

    render(<StemPlayer stems={mockStems} />);
    expect(screen.getByText("controls.restart")).toBeInTheDocument();
  });

  it("renders play button with correct aria-label when ended", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      ended: true,
    });

    render(<StemPlayer stems={mockStems} />);
    const playButton = screen.getByLabelText("controls.aria.restart");
    expect(playButton).toHaveAttribute("aria-label", "controls.aria.restart");
    expect(playButton).toHaveAttribute("title", "controls.title.restart");
  });

  it("renders stop button with correct aria-label when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer stems={mockStems} />);
    const stopButton = screen.getByLabelText("controls.aria.stop");
    expect(stopButton).toHaveAttribute("aria-label", "controls.aria.stop");
    expect(stopButton).toHaveAttribute("title", "controls.title.stop");
  });

  it("renders play button with aria-pressed false when not playing", () => {
    render(<StemPlayer stems={mockStems} />);
    const playButton = screen.getByLabelText("controls.aria.play");
    expect(playButton).toHaveAttribute("aria-pressed", "false");
  });

  it("renders stop button with aria-pressed true when playing", () => {
    mockUseStemPlayer.mockReturnValue({
      ...defaultMockReturnValue,
      playing: true,
    });

    render(<StemPlayer stems={mockStems} />);
    const stopButton = screen.getByLabelText("controls.aria.stop");
    expect(stopButton).toHaveAttribute("aria-pressed", "true");
  });

  it("renders multiple stems correctly", () => {
    const multipleStems: Stem[] = [
      { name: "Drums", url: "/drums.mp3" },
      { name: "Bass", url: "/bass.mp3" },
      { name: "Guitar", url: "/guitar.mp3" },
    ];

    render(<StemPlayer stems={multipleStems} />);

    expect(screen.getByText("Drums")).toBeInTheDocument();
    expect(screen.getByText("Bass")).toBeInTheDocument();
    expect(screen.getByText("Guitar")).toBeInTheDocument();
  });

  it("handles empty stems array", () => {
    render(<StemPlayer stems={[]} />);

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
    render(<StemPlayer stems={mockStems} className="custom-class" />);

    const container = screen.getByText("controls.master").closest(".container");
    expect(container).toHaveClass("custom-class");
  });

  it("renders with additional props", () => {
    render(<StemPlayer stems={mockStems} data-testid="stem-player" />);

    const container = screen.getByTestId("stem-player");
    expect(container).toBeInTheDocument();
  });
});
