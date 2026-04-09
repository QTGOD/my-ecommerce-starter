import { NavLink } from "react-router-dom";

const navItems = [
  { to: "/", label: "首页" },
  { to: "/products", label: "商品列表" },
];

export function SiteHeader() {
  return (
    <header className="site-header">
      <div className="site-header__inner">
        <NavLink className="brand" to="/">
          <span className="brand-mark">M</span>
          <div>
            <strong>My Ecommerce Starter</strong>
            <p>学前端结构、页面和联调的练手项目</p>
          </div>
        </NavLink>

        <nav className="site-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                isActive ? "site-nav__link site-nav__link--active" : "site-nav__link"
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
