"""Local preview server for the PicoHRT site.

The built HTML uses an absolute base path of `/PicoHRT/` (for GitHub Pages
hosting at https://sakanana001.github.io/PicoHRT/). For local preview,
this script strips the prefix from incoming requests and serves the file
out of site/dist/.

Usage:
  python tools/_serve_local.py [port]   # default 4321

Then open http://localhost:<port>/PicoHRT/ in your browser.
"""
import os
import sys
import posixpath
from http.server import HTTPServer, SimpleHTTPRequestHandler

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'site', 'dist'))
PREFIX = '/PicoHRT'


class StripPrefixHandler(SimpleHTTPRequestHandler):
    def translate_path(self, path: str) -> str:
        # Drop the /PicoHRT prefix so requests map to site/dist/*.
        stripped = path
        if stripped.startswith(PREFIX):
            stripped = stripped[len(PREFIX):]
        if not stripped or stripped == '/':
            stripped = '/index.html'
        # Build an absolute path under ROOT and normalize.
        full = os.path.normpath(os.path.join(ROOT, stripped.lstrip('/')))
        if not full.startswith(ROOT):
            return ROOT  # block path traversal
        return full

    def log_message(self, format: str, *args) -> None:
        sys.stderr.write(
            "[serve] %s - %s\n" % (self.address_string(), format % args)
        )


def main() -> None:
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 4321
    bind = '0.0.0.0' if '--lan' in sys.argv else '127.0.0.1'
    print(f"PicoHRT local preview: http://localhost:{port}{PREFIX}/")
    if bind == '0.0.0.0':
        print(f"  LAN mode (bound on {bind}:{port}). Other devices on the same network can use http://<your-machine-ip>:{port}{PREFIX}/")
    print(f"  serving from: {ROOT}")
    print(f"  prefix stripped: {PREFIX}")
    print(f"  press Ctrl-C to stop")
    server = HTTPServer((bind, port), StripPrefixHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[serve] stopped")


if __name__ == '__main__':
    main()
