import unittest

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import NoSuchElementException

from settings import WT_ID, WT_PWD, WT_URL
from login import login


class HROTests(unittest.TestCase):

    def setUp(self):
        chrome_options = Options()
        self.browser = webdriver.Chrome(chrome_options=chrome_options)
        login(self.browser, ident=WT_ID, pwd=WT_PWD, url=WT_URL)
        self.browser.find_element_by_css_selector(
            'button[aria-label="HRO role"]').click()
        self.browser.find_element_by_id('main-container').click()

    def tearDown(self):
        pass

    def test_upload_form(self):
        upload_form = self.browser.find_element_by_id('upload-form')

        file_input = upload_form.find_element_by_name('file')
        self.assertIsNotNone(file_input)
        self.assertEqual(file_input.get_attribute('accept'), '.pdf')

        self.assertIsNotNone(upload_form.find_element_by_name('path'))
        self.assertIsNotNone(upload_form.find_element_by_id('upload-btn'))

    def test_table_renders(self):
        try:
            cert_table = self.browser.find_element_by_css_selector(
                'table.jva-list.col-xs-12')
        except NoSuchElementException as err:
            self.assertIsNone(err)

        table_body = cert_table.find_elements_by_css_selector(
            'tr.row.jva-list-row')

        # Multiple rows
        self.assertGreater(len(table_body), 2)

        for row in table_body:
            # Every row has a header
            self.assertIsNotNone(
                row.find_element_by_css_selector('th.custom-col-4'))

            # Every row has 6 data columns OR is all header columns
            self.assertTrue(
                len(row.find_elements_by_css_selector('td')) == 7 or
                len(row.find_elements_by_css_selector('th')) == 8)

    def test_search_form(self):
        pass

    def test_edit_modal(self):
        pass
