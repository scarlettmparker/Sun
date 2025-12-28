type MenuItem = {
  name: string;
  href?: string;
};

type MenuType = Record<string, MenuItem>;

const menu: MenuType = {
  home: {
    name: "Home",
    href: "/",
  },
  blog: {
    name: "Blog",
  },
  gallery: {
    name: "Gallery",
  },
  // "stem-player": {
  //   name: "Stem Player",
  // },
};

export default menu;
