import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Link from "next/link";
import { Radar } from "lucide-react";
import "./globals.css";

/*
 * Inter stands in for Sohne, which Stripe licenses and this cannot. It is the usual substitute
 * and gets close on the things that matter here: a tall x-height, unambiguous digits, and a
 * tight fit at small sizes, which is most of what a console renders.
 */
const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "korea-job-intelligence",
    template: "%s — korea-job-intelligence",
  },
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
    <html lang="en" className={`${inter.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col bg-background text-foreground">
        {/*
          A sticky top bar rather than a fixed sidebar. The widest thing here is a fourteen
          column table, and a rail permanently spends 220px of the only axis that table needs.
        */}
        <header className="sticky top-0 z-40 w-full border-b bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/70">
          <div className="mx-auto flex h-14 max-w-[110rem] items-center gap-4 px-4 sm:px-6">
            <Link
              href="/"
              className="flex shrink-0 items-center gap-2 text-sm font-semibold tracking-tight"
            >
              <Radar className="h-[18px] w-[18px] text-brand" aria-hidden="true" />
              <span>korea-job-intelligence</span>
            </Link>
            <nav className="ml-1 hidden items-center gap-0.5 md:flex">
              {NAV.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className="rounded-md px-2.5 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                >
                  {item.label}
                </Link>
              ))}
            </nav>
            <span className="ml-auto hidden text-xs text-muted-foreground sm:inline">
              operator console
            </span>
          </div>
          {/* Below md the bar cannot hold six links, so they wrap into a scrollable second row
              rather than hiding behind a menu this console does not need. */}
          <nav className="flex items-center gap-0.5 overflow-x-auto border-t px-4 py-1.5 md:hidden">
            {NAV.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="shrink-0 rounded-md px-2.5 py-1 text-sm text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </header>
        <main className="flex-1">{children}</main>
      </body>
    </html>
  );
}
