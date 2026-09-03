import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "korea-job-intelligence",
  description: "Korean software job discovery, intelligence and application tracking",
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
        <div className="layout">
          <aside className="sidebar">
            <div className="sidebar-brand">
              korea-job-intelligence
              <span>operator console</span>
            </div>
            <nav>
              {NAV.map((item) => (
                <Link key={item.href} href={item.href}>
                  {item.label}
                </Link>
              ))}
            </nav>
          </aside>
          <main className="content">{children}</main>
        </div>
      </body>
    </html>
  );
}
