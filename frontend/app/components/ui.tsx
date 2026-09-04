import type { ReactNode } from "react";
import { cn } from "../lib/cn";

/*
 * A small set of primitives in Stripe's idiom.
 *
 * Deliberately hand-written rather than pulled from a component library. Untouched library
 * defaults are what makes an interface look generated: large radii, heavy shadows, a saturated
 * accent used as a fill. These go the other way — small radii, hairline borders, one indigo
 * used only for links and the primary action, and semantic colour only ever as dark text on a
 * pale tint.
 *
 * Nothing here replaces a native form control. The console's forms post to server actions and
 * work with JavaScript disabled, which only holds while the fields are real inputs and selects.
 */

/** The page container. Width follows the page's job: a table needs more room than prose. */
export function Container({
  width = "wide",
  className,
  children,
}: {
  width?: "wide" | "table" | "detail" | "prose";
  className?: string;
  children: ReactNode;
}) {
  const max = {
    wide: "max-w-7xl",
    table: "max-w-[110rem]",
    detail: "max-w-5xl",
    prose: "max-w-3xl",
  }[width];
  return <div className={cn("mx-auto px-4 py-8 sm:px-6", max, className)}>{children}</div>;
}

/** A page heading, its one-line explanation, and anything that belongs opposite them. */
export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {subtitle ? <p className="mt-1 mb-6 text-sm text-muted-foreground">{subtitle}</p> : null}
      </div>
      {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
    </div>
  );
}

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn("rounded-lg border bg-surface", className)}>{children}</div>;
}

export function CardHeader({ title, hint }: { title: ReactNode; hint?: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-3 border-b px-4 py-3">
      <h2 className="text-sm font-semibold tracking-tight">{title}</h2>
      {hint ? <span className="text-xs text-muted-foreground">{hint}</span> : null}
    </div>
  );
}

/** One number and what it counts. */
export function Stat({
  label,
  value,
  sub,
}: {
  label: string;
  value: ReactNode;
  sub?: ReactNode;
}) {
  return (
    <div className="rounded-lg border bg-surface px-4 py-3">
      <div className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
        {label}
      </div>
      <div className="mt-1 text-2xl font-semibold tracking-tight tabular-nums">{value}</div>
      {sub ? <div className="mt-0.5 text-xs text-muted-foreground">{sub}</div> : null}
    </div>
  );
}

type Tone = "neutral" | "good" | "warn" | "bad" | "brand";

const TONES: Record<Tone, string> = {
  // Dark text on a pale tint. A saturated fill would shout, and a table full of shouting
  // badges is unreadable.
  neutral: "bg-neutral-badge-subtle text-neutral-badge ring-border-strong/60",
  good: "bg-good-subtle text-good ring-good/20",
  warn: "bg-warn-subtle text-warn ring-warn/20",
  bad: "bg-bad-subtle text-bad ring-bad/20",
  brand: "bg-brand-subtle text-brand ring-brand/20",
};

export function Badge({
  tone = "neutral",
  className,
  children,
}: {
  tone?: Tone;
  className?: string;
  children: ReactNode;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md px-1.5 py-0.5 text-xs font-medium whitespace-nowrap ring-1 ring-inset",
        TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

/** Shared field chrome, so a native input and a native select look like siblings. */
export const controlClass =
  "h-9 w-full rounded-md border bg-surface px-2.5 text-sm text-foreground " +
  "placeholder:text-muted-foreground/70 " +
  "focus:border-border-strong focus:outline-none focus:ring-2 focus:ring-ring/40 " +
  "disabled:cursor-not-allowed disabled:opacity-60";

export const buttonClass =
  "inline-flex h-9 items-center justify-center gap-1.5 rounded-md border border-transparent " +
  "bg-brand px-3 text-sm font-medium whitespace-nowrap text-brand-foreground " +
  "hover:brightness-95 focus:outline-none focus:ring-2 focus:ring-ring/40 " +
  "disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:brightness-100";

export const secondaryButtonClass =
  "inline-flex h-9 items-center justify-center gap-1.5 rounded-md border bg-surface px-3 " +
  "text-sm font-medium whitespace-nowrap text-foreground hover:bg-muted " +
  "focus:outline-none focus:ring-2 focus:ring-ring/40";

/** A labelled control. The label is small and quiet; the control carries the weight. */
export function Field({
  label,
  htmlFor,
  className,
  children,
}: {
  label: string;
  htmlFor?: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <label htmlFor={htmlFor} className={cn("flex flex-col gap-1", className)}>
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      {children}
    </label>
  );
}

/*
 * Table chrome.
 *
 * Every cell is nowrap. A dense table earns its density by keeping one row on one line: the
 * moment a Korean company name or a full street address is allowed to wrap, rows range from one
 * to five lines and the eye loses the row entirely. Long values truncate with a title attribute,
 * so nothing is actually lost.
 */
export function TableWrap({ children }: { children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-lg border bg-surface">
      {/*
        The row rule and the hover tint live here rather than on each page's tbody, so every
        table in the console separates its rows the same way. A single hairline between rows is
        the whole furniture budget: no vertical rules, and no zebra striping, which would spend
        the background on a pattern and leave hover with nothing to say.
      */}
      <table className="w-full text-sm [&_tbody_tr:first-child]:border-t-0 [&_tbody_tr]:border-t [&_tbody_tr]:transition-colors hover:[&_tbody_tr]:bg-muted/40">
        {children}
      </table>
    </div>
  );
}

export function Th({
  className,
  align = "left",
  children,
}: {
  className?: string;
  align?: "left" | "right";
  children: ReactNode;
}) {
  return (
    <th
      scope="col"
      className={cn(
        "whitespace-nowrap border-b bg-subtle px-3 py-2 text-xs font-medium uppercase tracking-wide text-muted-foreground",
        align === "right" ? "text-right" : "text-left",
        className,
      )}
    >
      {children}
    </th>
  );
}

export function Td({
  className,
  align = "left",
  title,
  colSpan,
  children,
}: {
  className?: string;
  align?: "left" | "right";
  title?: string;
  colSpan?: number;
  children: ReactNode;
}) {
  return (
    <td
      title={title}
      colSpan={colSpan}
      className={cn(
        "whitespace-nowrap px-3 py-2 align-middle",
        align === "right" ? "text-right tabular-nums" : "",
        className,
      )}
    >
      {children}
    </td>
  );
}

/** Caps a cell's width and truncates, keeping the full value in the tooltip. */
export function Truncate({
  value,
  width = "12rem",
}: {
  value: string | null | undefined;
  width?: string;
}) {
  if (!value) {
    return <span className="text-muted-foreground/70">—</span>;
  }
  return (
    <span className="block truncate" style={{ maxWidth: width }} title={value}>
      {value}
    </span>
  );
}

/*
 * The console's word for "nothing was established".
 *
 * It is deliberately quiet: an operator scanning a column needs the values to stand out, and a
 * gap is not a value. The formatters in lib/format return the same word as a plain string when
 * they are handed a null, so this treats that literal as the gap it stands for rather than as
 * text someone wrote — otherwise the identical word renders at two different weights depending
 * on which side of the formatter it came from.
 */
export function Unknown() {
  return <span className="text-muted-foreground/70">unknown</span>;
}

export function OrUnknown({ value }: { value: string | number | null | undefined }) {
  if (value === null || value === undefined || value === "" || value === "unknown") {
    return <Unknown />;
  }
  return <>{value}</>;
}

/** One label and one value, for the definition grids that head the detail pages. */
export function Fact({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <div className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
        {label}
      </div>
      <div className="mt-0.5">
        {typeof value === "string" || value === null || value === undefined ? (
          <OrUnknown value={value} />
        ) : (
          value
        )}
      </div>
    </div>
  );
}

/** The definition grid itself. Four across on a wide screen, two on a narrow one. */
export function FactGrid({ children }: { children: ReactNode }) {
  return (
    <div className="grid grid-cols-2 gap-x-6 gap-y-3 rounded-lg border bg-surface p-4 sm:grid-cols-3 lg:grid-cols-4">
      {children}
    </div>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="px-4 py-10 text-center text-sm text-muted-foreground">{children}</div>;
}
