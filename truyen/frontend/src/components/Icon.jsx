const PATHS = {
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14ZM21 21l-5-5',
  bell: 'M6 16V11a6 6 0 1 1 12 0v5l2 2H4l2-2ZM10 21h4',
  sun: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8ZM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4',
  moon: 'M20 14.5A8.5 8.5 0 1 1 9.5 4a7 7 0 0 0 10.5 10.5Z',
  home: 'M4 11l8-7 8 7v9h-5v-6H9v6H4v-9Z',
  grid: 'M4 4h7v7H4V4ZM13 4h7v7h-7V4ZM4 13h7v7H4v-7ZM13 13h7v7h-7v-7Z',
  book: 'M5 4h6a2 2 0 0 1 1 .3A2 2 0 0 1 13 4h6v15h-6a2 2 0 0 0-1 .3 2 2 0 0 0-1-.3H5V4ZM12 4.3v15',
  user: 'M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8ZM4 21c0-4 3.6-7 8-7s8 3 8 7',
  heart: 'M12 20s-8-5-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 9c0 6-8 11-8 11Z',
  star: 'M12 3l2.8 5.8 6.2.9-4.5 4.4 1 6.3-5.5-3-5.5 3 1-6.3L3 9.7l6.2-.9L12 3Z',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7ZM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z',
  left: 'M15 5l-7 7 7 7',
  right: 'M9 5l7 7-7 7',
  up: 'M5 15l7-7 7 7',
  down: 'M5 9l7 7 7-7',
  x: 'M6 6l12 12M18 6L6 18',
  menu: 'M4 7h16M4 12h16M4 17h16',
  flag: 'M6 21V4h11l-2 4 2 4H6',
  trash: 'M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13',
  reply: 'M10 7L4 12l6 5v-3c4 0 7 1 10 5-1-6-4-9-10-9V7Z',
  logout: 'M15 4h4v16h-4M10 8l-4 4 4 4M6 12h9',
  plus: 'M12 5v14M5 12h14',
  edit: 'M4 20l1-4L16 5l3 3L8 19l-4 1Z',
  upload: 'M12 16V5M7 10l5-5 5 5M4 20h16',
  chart: 'M4 20V4M4 20h16M8 16v-4M13 16V8M18 16v-7',
  tag: 'M4 4h8l8 8-8 8-8-8V4ZM8 8h.01',
  pen: 'M4 20l4-1L19 8l-3-3L5 16l-1 4ZM14 7l3 3',
  users: 'M9 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8ZM2 20c0-3.5 3-6 7-6s7 2.5 7 6M16 4.5a4 4 0 0 1 0 7M18 14.5c2.4.7 4 2.7 4 5.5',
  log: 'M6 3h9l4 4v14H6V3ZM9 11h7M9 15h7',
  fire: 'M12 3c1 4 6 6 6 11a6 6 0 0 1-12 0c0-2 1-3.5 2-4.5 0 2 1 3 2 3 1-3-.5-6 2-9.5Z',
  list: 'M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01',
}

export default function Icon({ name, size = 20, filled = false }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth="2.2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  )
}
