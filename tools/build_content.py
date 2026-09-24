"""Build the two authored continuous textbooks without regenerating other tracks."""
from compile_track3_book import build
from compile_track4_book import build as build_track4

if __name__ == "__main__":
    build()
    build_track4()
