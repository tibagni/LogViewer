#!/usr/bin/env python3
"""Unit tests for check_i18n.py."""

import unittest
from check_i18n import find_hardcoded_in_line, is_exempt


class CheckI18nTests(unittest.TestCase):

    def test_detects_set_text(self):
        line = '  myLabel.setText("Hello World");'
        self.assertEqual(find_hardcoded_in_line(line), ["Hello World"])

    def test_detects_new_jbutton(self):
        line = '  JButton btn = new JButton("Save");'
        self.assertEqual(find_hardcoded_in_line(line), ["Save"])

    def test_detects_new_jlabel(self):
        line = '  JLabel label = new JLabel("Status");'
        self.assertEqual(find_hardcoded_in_line(line), ["Status"])

    def test_detects_titled_border(self):
        line = '  new TitledBorder("Options");'
        self.assertEqual(find_hardcoded_in_line(line), ["Options"])

    def test_ignores_i18n_calls(self):
        line = '  myLabel.setText(I18n.get(I18n.PREF_LOOK_AND_FEEL));'
        self.assertEqual(find_hardcoded_in_line(line), [])

    def test_ignores_exempt_values(self):
        line1 = '  myLabel.setText("");'
        line2 = '  btn.setText("...");'
        self.assertEqual(find_hardcoded_in_line(line1), [])
        self.assertEqual(find_hardcoded_in_line(line2), [])

    def test_ignores_comments_and_ignore_annotations(self):
        line1 = '  // myLabel.setText("Old text");'
        line2 = '  myLabel.setText("Raw") // i18n:ignore'
        self.assertEqual(find_hardcoded_in_line(line1), [])
        self.assertEqual(find_hardcoded_in_line(line2), [])


if __name__ == "__main__":
    unittest.main()
