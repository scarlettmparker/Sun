import Button from "../button";
import { ArrowDownIcon, ArrowUpIcon } from "@heroicons/react/24/outline";
import { cn } from "~/utils/cn";
import styles from "./vote-control.module.css";

export type VoteDirection = "UP" | "DOWN";

type VoteControlProps = {
  /**
   * Total upvote count.
   */
  upvotes: number;
  /**
   * Total downvote count.
   */
  downvotes: number;
  /**
   * The caller's current vote, or null.
   */
  myVote?: VoteDirection | null;
  /**
   * Called when a vote button is pressed.
   */
  onVote: (value: VoteDirection) => void;
} & Omit<React.HTMLAttributes<HTMLElement>, "children">;

/**
 * Up/down vote control with counts and the caller's vote highlighted.
 */
const VoteControl = ({
  upvotes,
  downvotes,
  myVote,
  onVote,
  className,
  ...rest
}: VoteControlProps) => {
  const ICON = 16;
  return (
    <div className={cn(styles.vote_control, className)} {...rest}>
      <Button
        variant={myVote === "UP" ? "default" : "secondary"}
        onClick={() => onVote("UP")}
        aria-label="Upvote"
      >
        <ArrowUpIcon width={ICON} height={ICON} />
        {upvotes}
      </Button>
      <Button
        variant={myVote === "DOWN" ? "default" : "secondary"}
        onClick={() => onVote("DOWN")}
        aria-label="Downvote"
      >
        <ArrowDownIcon width={ICON} height={ICON} />
        {downvotes}
      </Button>
    </div>
  );
};

export default VoteControl;
