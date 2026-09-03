import { Suspense } from "react";
import { Card } from "@sun/components";
import { ProfileCardSkeleton } from "./skeletons";
import ProfileCardContent from "./profile-card-content";

type ProfileCardProps = React.HTMLAttributes<HTMLDivElement>;

/**
 * Profile card shell wrapping suspense.
 */
const ProfileCard = (props: ProfileCardProps) => {
  const { className, ...rest } = props;

  return (
    <Card className={className} {...rest}>
      <Suspense fallback={<ProfileCardSkeleton />}>
        <ProfileCardContent />
      </Suspense>
    </Card>
  );
};

export default ProfileCard;
