import unittest
from login import login
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import NoSuchElementException

from settings import WT_ID, WT_PWD, WT_URL


class CertificationTests(unittest.TestCase):

    def setUp(self):
        chrome_options = Options()
        # chrome_options.add_argument('--headless')
        self.browser = webdriver.Chrome(chrome_options=chrome_options)
        login(self.browser, ident=WT_ID, pwd=WT_PWD, url=WT_URL)
        self.browser.find_element_by_css_selector(
            'button[aria-label="Certification role"]').click()
        self.browser.find_element_by_id('main-container').click()

    def tearDown(self):
        self.browser.close()

    def test_table_rendered(self):
        try:
            cert_table = self.browser.find_element_by_css_selector(
                'table.lookup-list.col-xs-12')
        except NoSuchElementException as err:
            self.assertIsNone(err)

        table_body = cert_table.find_elements_by_css_selector(
            'tr.row.lookup-row')

        # Multiple rows
        self.assertGreater(len(table_body), 2)

        for row in table_body:
            # Every row has a header
            self.assertIsNotNone(
                row.find_element_by_css_selector('th.custom-col-3'))

            # Every row has 6 data columns OR is all header columns
            self.assertTrue(
                len(row.find_elements_by_css_selector('td')) == 6 or len(row.find_elements_by_css_selector('th')) == 7)

    def test_upload_form_exists(self):
        upload_form = self.browser.find_element_by_id('upload-form')

        self.assertIsNotNone(upload_form.find_element_by_name('file'))
        self.assertIsNotNone(upload_form.find_element_by_name('path'))
        self.assertIsNotNone(upload_form.find_element_by_id('upload-btn'))

    def test_edit_modals(self):
        pass

    def test_delete_buttons(self):
        pass

    def test_search_form(self):
        pass
