"""Rebuild approved textbooks and the independent Vibe Coding glossary.

All existing Track 1-4 stable identities and progress remain unchanged.
"""
from compile_track3_book import build as build_track3
from compile_track12_glossary import build as build_vibe
from compile_track4_book import build as build_track4

if __name__ == "__main__":
    build_track3()
    build_vibe()
    build_track4()
