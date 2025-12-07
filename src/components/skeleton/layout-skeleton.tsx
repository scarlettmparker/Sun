import Skeleton from "./skeleton";

/**
 * Skeleton component for the layout during loading states.
 */
const LayoutSkeleton = () => {
  return (
    <>
      {/* Skeleton for TopNavBar */}
      <nav
        style={{
          display: "flex",
          gap: "1rem",
          padding: "1rem",
          borderBottom: "1px solid #ccc",
        }}
      >
        <Skeleton style={{ width: "100px", height: "40px" }} />
        <Skeleton style={{ width: "100px", height: "40px" }} />
        <Skeleton style={{ width: "100px", height: "40px" }} />
      </nav>
      {/* Skeleton for main content */}
      <main style={{ padding: "2rem" }}>
        <Skeleton
          style={{ width: "100%", height: "200px", marginBottom: "1rem" }}
        />
        <Skeleton
          style={{ width: "80%", height: "20px", marginBottom: "0.5rem" }}
        />
        <Skeleton
          style={{ width: "60%", height: "20px", marginBottom: "0.5rem" }}
        />
        <Skeleton style={{ width: "90%", height: "20px" }} />
      </main>
    </>
  );
};

export default LayoutSkeleton;
