export type Project = {
  /**
   * Unique key.
   */
  key: string;
  /**
   * Display title.
   */
  title: string;
  /**
   * External href or null when disabled (current site).
   */
  href: string | null;
  /**
   * Visit href for live site when different from github.
   */
  visitHref?: string | null;
  /**
   * i18n key for description.
   */
  descriptionKey: string;
  /**
   * i18n key for markdown detail shown in dialog.
   */
  detailKey: string;
  /**
   * Sort rank.
   */
  rank: number;
  /**
   * Whether card is disabled.
   */
  disabled?: boolean;
};

export const personalProjects: Project[] = [
  {
    key: "sun",
    title: "Sun",
    href: "https://github.com/scarlettmparker/Sun",
    descriptionKey: "projects.sun.description",
    detailKey: "projects.sun.detail",
    rank: 10,
    disabled: true,
  },
  {
    key: "guided-reader",
    title: "Guided Reader",
    href: "https://github.com/scarlettmparker/Guided-Reader",
    visitHref: "https://reader.scarlettparker.co.uk",
    descriptionKey: "projects.guided-reader.description",
    detailKey: "projects.guided-reader.detail",
    rank: 20,
  },
  {
    key: "niece-scarlett",
    title: "Niece Scarlett",
    href: "https://github.com/scarlettmparker/Niece-Scarlett",
    descriptionKey: "projects.niece-scarlett.description",
    detailKey: "projects.niece-scarlett.detail",
    rank: 30,
  },
  {
    key: "learner",
    title: "Learner",
    href: "https://github.com/scarlettmparker/learner",
    descriptionKey: "projects.learner.description",
    detailKey: "projects.learner.detail",
    rank: 40,
  },
];

export const internalProjects: Project[] = [
  {
    key: "filestore",
    title: "Filestore",
    href: "https://github.com/scarlettmparker/Filestore",
    descriptionKey: "projects.filestore.description",
    detailKey: "projects.filestore.detail",
    rank: 10,
  },
  {
    key: "checklist",
    title: "Checklist",
    href: "https://github.com/scarlettmparker/Checklist",
    descriptionKey: "projects.checklist.description",
    detailKey: "projects.checklist.detail",
    rank: 20,
  },
];
