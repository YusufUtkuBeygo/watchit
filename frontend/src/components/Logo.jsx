//Site logosu: sekme simgesiyle (favicon) aynı sarı kare + W, yanında "watchit".
//Dar ekranda yazı gizlenir (Navbar.css), kare kalır.
export default function Logo() {
  return (
    <>
      <svg className="logo-mark" width="28" height="28" viewBox="0 0 32 32" aria-hidden="true" focusable="false">
        <rect width="32" height="32" rx="7" fill="var(--accent)" />
        <path d="M7 10l3.6 12L16 13l5.4 9L25 10" fill="none" stroke="var(--on-accent)" strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      <span className="logo-text">watchit</span>
    </>
  );
}
