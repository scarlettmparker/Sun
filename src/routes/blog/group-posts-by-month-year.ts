import { BlogPost } from "~/generated/graphql";

export type GroupedPost = { monthYear: string; posts: BlogPost[] };
/**
 * Groups blog posts by month and year in descending order.
 *
 * @param posts - Array of blog posts, potentially with null values.
 * @returns Array of groups, each containing a monthYear string (e.g., "December 2025")
 * and an array of posts for that month/year.
 */
export function groupPostsByMonthYear(
  posts: (BlogPost | null)[]
): GroupedPost[] {
  const monthNames = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];
  const groups = new Map<string, BlogPost[]>();

  posts.forEach((post) => {
    if (!post || !post.createdAt) return;
    const date = new Date(post.createdAt);
    if (isNaN(date.getTime())) return;
    const year = date.getFullYear();
    const month = monthNames[date.getMonth()];
    const key = `${month} ${year}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(post);
  });

  // Sort groups by year descending, then month descending
  const sortedGroups = Array.from(groups.entries()).sort(([a], [b]) => {
    const [monthA, yearA] = a.split(" ");
    const [monthB, yearB] = b.split(" ");

    if (yearA !== yearB) return parseInt(yearB) - parseInt(yearA);
    return monthNames.indexOf(monthB) - monthNames.indexOf(monthA);
  });

  return sortedGroups.map(([monthYear, posts]) => ({ monthYear, posts }));
}
