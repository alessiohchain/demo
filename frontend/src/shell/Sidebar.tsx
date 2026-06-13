import { Fragment, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ChevronDown, ChevronRight, Search } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import type { MenuItem } from '@/app/auth/AuthProvider';
import { cn } from '@/lib/utils';
import { clearBreadcrumbs } from '@alessiohchain/csnx-engine/breadcrumbs/breadcrumbStore';
import { resetWorkflowHistory } from '@alessiohchain/csnx-engine/process/workflowTracker';
import { startFreshScreenFlow } from '@alessiohchain/csnx-engine/screen/screenFlow';
import { iconForMenuItem } from '@alessiohchain/csnx-engine/icons/menuIcons';

interface Props {
  items: MenuItem[];
}

export function Sidebar({ items }: Props) {
  const [filter, setFilter] = useState('');
  const filtered = useMemo(() => filterTree(items, filter.trim().toLowerCase()), [items, filter]);

  if (items.length === 0) {
    return (
      <div className="p-4 text-sm text-muted-foreground">No menu items available.</div>
    );
  }

  return (
    <nav className="flex h-full flex-col">
      <div className="border-b p-2">
        <div className="relative">
          <Search
            className="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground"
            aria-hidden
          />
          <input
            type="search"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Search menu…"
            className="h-8 w-full rounded-md border border-input bg-background pl-7 pr-2 text-sm transition-colors hover:border-primary hover:bg-primary/5 focus:outline-hidden focus:border-ring focus:bg-background focus:ring-2 focus:ring-ring/30"
            aria-label="Search menu"
          />
        </div>
      </div>
      <ul className="flex-1 overflow-y-auto p-2 space-y-0.5">
        {filtered.length === 0 ? (
          <li className="px-2 py-1.5 text-xs text-muted-foreground">No matches.</li>
        ) : (
          filtered.map((item) => (
            <SidebarNode
              key={item.code}
              item={item}
              depth={0}
              forceOpen={filter.length > 0}
            />
          ))
        )}
      </ul>
    </nav>
  );
}

function SidebarNode({
  item,
  depth,
  forceOpen,
}: {
  item: MenuItem;
  depth: number;
  forceOpen: boolean;
}) {
  const [openState, setOpenState] = useState(false);
  const open = forceOpen ? true : openState;
  const hasChildren = !!item.children?.length;
  const location = useLocation();
  const isActive =
    !!item.fastpath &&
    location.pathname.toUpperCase() === `/${item.fastpath.toUpperCase()}`;

  const navigate = useNavigate();
  const queryClient = useQueryClient();
  // Resolve a logical lucide icon for this entry — group or leaf.
  // Keyword-matched against description/code/fastpath; falls back to
  // Folder (group) / FileText (leaf) so every row carries a glyph.
  const Icon = iconForMenuItem({
    description: item.description,
    code: item.code,
    fastpath: item.fastpath,
    isGroup: hasChildren,
  });

  if (!hasChildren && item.fastpath) {
    return (
      <li>
        <Link
          to={`/${item.fastpath}`}
          // A menu click is a fresh top-level workflow start — same
          // semantics as a fastpath input. Mirrors GWT's
          // {@code changePage(workflow, true, true)}: clear breadcrumb +
          // workflow tracker, then replace the current browser entry so
          // the prior screen leaves the back-stack entirely. Without
          // {@code replace}, Close on the destination would navigate(-1)
          // into the previous flow instead of going to /.
          onClick={(e) => {
            e.preventDefault();
            clearBreadcrumbs();
            resetWorkflowHistory();
            startFreshScreenFlow(queryClient);
            navigate(`/${item.fastpath}`, { replace: true });
          }}
          className={cn(
            'group flex items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm transition-colors hover:bg-primary/5 hover:text-foreground',
            isActive
              ? 'bg-primary/10 text-primary font-medium'
              : 'text-foreground/80',
          )}
          style={{ paddingLeft: `${depth * 12 + 8}px` }}
          title={item.code}
        >
          <Icon
            className={cn(
              'h-4 w-4 shrink-0',
              isActive ? 'text-primary' : 'text-muted-foreground group-hover:text-foreground',
            )}
            aria-hidden
          />
          <span className="flex-1 truncate">{item.description}</span>
          {/* Fastpath code as a small monospace hint on the right — same
              way command palettes show keyboard shortcuts. Helps users
              learn the codes they'll later type into the header
              fastpath input. */}
          <span
            className={cn(
              'shrink-0 font-mono text-[10px] tracking-wide opacity-0 group-hover:opacity-70 transition-opacity',
              isActive && 'opacity-70',
            )}
          >
            {item.fastpath.toUpperCase()}
          </span>
        </Link>
      </li>
    );
  }

  // Top-level groups read as small-caps section headers; nested groups
  // keep the default weight so the hierarchy is obvious at a glance.
  const isTopGroup = depth === 0;
  return (
    <Fragment>
      <li>
        <button
          type="button"
          onClick={() => setOpenState((v) => !v)}
          className={cn(
            'flex w-full items-center gap-2 rounded-md px-2 py-1.5 transition-colors hover:bg-primary/5',
            isTopGroup
              ? 'text-[11px] font-semibold uppercase tracking-wider text-foreground mt-2'
              : 'text-sm font-medium text-foreground/80',
          )}
          style={{ paddingLeft: `${depth * 12 + 4}px` }}
          aria-expanded={open}
        >
          {open ? (
            <ChevronDown className="h-3.5 w-3.5 shrink-0 text-muted-foreground" aria-hidden />
          ) : (
            <ChevronRight className="h-3.5 w-3.5 shrink-0 text-muted-foreground" aria-hidden />
          )}
          {/* Section icon — top-level groups display the icon in the
              active accent so the eye lands on them first; nested
              groups keep the icon muted so the tree still reads as a
              hierarchy. */}
          <Icon
            className={cn(
              'shrink-0',
              isTopGroup ? 'h-4 w-4 text-primary' : 'h-3.5 w-3.5 text-muted-foreground',
            )}
            aria-hidden
          />
          <span className="truncate">{item.description}</span>
        </button>
      </li>
      {open && hasChildren && (
        <li>
          <ul className="space-y-0.5">
            {item.children!.map((child) => (
              <SidebarNode
                key={child.code}
                item={child}
                depth={depth + 1}
                forceOpen={forceOpen}
              />
            ))}
          </ul>
        </li>
      )}
    </Fragment>
  );
}

/**
 * Substring filter that keeps a parent in the tree if any descendant matches.
 * The empty filter returns the original tree.
 */
function filterTree(items: MenuItem[], q: string): MenuItem[] {
  if (!q) return items;
  const out: MenuItem[] = [];
  for (const item of items) {
    const selfMatch =
      item.description?.toLowerCase().includes(q) ||
      item.code?.toLowerCase().includes(q) ||
      item.fastpath?.toLowerCase().includes(q);
    const kidsMatched = filterTree(item.children ?? [], q);
    if (selfMatch || kidsMatched.length > 0) {
      out.push({ ...item, children: kidsMatched.length > 0 ? kidsMatched : item.children });
    }
  }
  return out;
}
