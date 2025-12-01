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
};

export default menu;
