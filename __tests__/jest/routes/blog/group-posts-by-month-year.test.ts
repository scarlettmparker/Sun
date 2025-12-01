import { BlogPost } from "~/generated/graphql";
import {
  groupPostsByMonthYear,
  GroupedPost,
} from "~/routes/blog/group-posts-by-month-year";

describe("groupPostsByMonthYear", () => {
  it("should group posts by month and year", () => {
    const posts: (BlogPost | null)[] = [
      {
        id: "1",
        title: "Post 1",
        createdAt: "2025-12-01T22:56:49.104963",
      },
      {
        id: "2",
        title: "Post 2",
        createdAt: "2025-11-15T10:00:00.000000",
      },
      {
        id: "3",
        title: "Post 3",
        createdAt: "2025-12-05T12:00:00.000000",
      },
      null,
      {
        id: "4",
        title: "Post 4",
        createdAt: null,
      },
    ];

    const result = groupPostsByMonthYear(posts) as GroupedPost[];

    expect(result).toHaveLength(2);
    expect(result[0]).toEqual({
      monthYear: "December 2025",
      posts: [
        {
          id: "1",
          title: "Post 1",
          createdAt: "2025-12-01T22:56:49.104963",
        },
        {
          id: "3",
          title: "Post 3",
          createdAt: "2025-12-05T12:00:00.000000",
        },
      ],
    });
    expect(result[1]).toEqual({
      monthYear: "November 2025",
      posts: [
        {
          id: "2",
          title: "Post 2",
          createdAt: "2025-11-15T10:00:00.000000",
        },
      ],
    });
  });

  it("should return empty array for no posts", () => {
    const result = groupPostsByMonthYear([]);
    expect(result).toEqual([]);
  });

  it("should handle posts with invalid dates", () => {
    const posts: (BlogPost | null)[] = [
      {
        id: "1",
        title: "Post 1",
        createdAt: "invalid",
      },
    ];

    const result = groupPostsByMonthYear(posts);
    expect(result).toEqual([]);
  });

  it("should sort groups by year descending, then month descending", () => {
    const posts: (BlogPost | null)[] = [
      {
        id: "1",
        title: "Post 1",
        createdAt: "2024-01-01T00:00:00.000000",
      },
      {
        id: "2",
        title: "Post 2",
        createdAt: "2025-12-01T00:00:00.000000",
      },
      {
        id: "3",
        title: "Post 3",
        createdAt: "2025-11-01T00:00:00.000000",
      },
    ];

    const result = groupPostsByMonthYear(posts);

    expect(result[0].monthYear).toBe("December 2025");
    expect(result[1].monthYear).toBe("November 2025");
    expect(result[2].monthYear).toBe("January 2024");
  });
});
