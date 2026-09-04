import type { Metadata, Viewport } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "korea-job-intelligence",
  description: "Korean software job discovery, intelligence and application tracking",
};

/**
 * The console is a light surface and says so.
 *
 * Without this, a browser on an OS dark theme renders the native date pickers and select
 * dropdowns in dark chrome on top of a white page. `colorScheme` also mirrors the
 * `color-scheme: light` declaration in globals.css, which is what the form controls read.
 */
export const viewport: Viewport = {
  colorScheme: "light",
  themeColor: "#ffffff",
};

const NAV = [
  { href: "/", label: "Dashboard" },
  { href: "/jobs", label: "Jobs" },
  { href: "/companies", label: "Companies" },
  { href: "/applications", label: "Applications" },
  { href: "/sources", label: "Sources" },
  { href: "/search-runs", label: "Search runs" },
];

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        {/* Six nav links stand between the keyboard and the table on every page. */}
        <a className="skip-link" href="#content">
          Skip to content
        </a>
        <div className="layout">
          <aside className="sidebar">
            <div className="sidebar-brand">
              korea-job-intelligence
              <span>operator console</span>
            </div>
            <nav aria-label="Console sections">
              {NAV.map((item) => (
                <Link key={item.href} href={item.href}>
                  {item.label}
                </Link>
              ))}
            </nav>
          </aside>
          <main className="content" id="content">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
