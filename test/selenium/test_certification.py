import unittest
import time
import logging
from login import login
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import (
    NoSuchElementException, WebDriverException)
from selenium.webdriver.common.keys import Keys

from settings import WT_ID, WT_PWD, WT_URL

TABLE_SELECTOR = 'table.lookup-list.col-xs-12'
TR_SELECTOR = 'tr.row.lookup-row'
DATA_CELLS_PER_ROW = 6
CELLS_PER_ROW = DATA_CELLS_PER_ROW + 1


class CertificationTests(unittest.TestCase):

    def setUp(self):
        chrome_options = Options()
        # chrome_options.add_argument('--headless')
        self.browser = webdriver.Chrome(chrome_options=chrome_options)
        login(self.browser, ident=WT_ID, pwd=WT_PWD, url=WT_URL)
        self.browser.find_element_by_css_selector(
            'button[aria-label="Certification role"]').click()
        self.browser.find_element_by_id('main-container').click()

        self.logger = logging.getLogger('testLogger')
        self.logger.setLevel(logging.DEBUG)
        fh = logging.FileHandler('test.log')
        fh.setLevel(logging.DEBUG)
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        fh.setFormatter(formatter)
        self.logger.addHandler(fh)

    def tearDown(self):
        self.browser.close()

    def test_table_renders(self):
        """
        Tests that the page renders a table containing records of Teacher and
        staff BOE certifications.
        """
        try:
            cert_table = self.browser.find_element_by_css_selector(
                TABLE_SELECTOR)
        except NoSuchElementException as err:
            self.assertIsNone(err)

        table_body = cert_table.find_elements_by_css_selector(
            TR_SELECTOR)

        # Multiple rows
        self.assertGreater(len(table_body), 1)

        for row in table_body:
            # Every row has a header
            self.assertIsNotNone(
                row.find_element_by_css_selector('th'))

            # Every row has the correct number of data columns OR is all header
            # columns
            n_data = len(row.find_elements_by_css_selector('td'))
            n_headers = len(row.find_elements_by_css_selector('th'))
            self.assertTrue(
                n_data == DATA_CELLS_PER_ROW or n_headers == CELLS_PER_ROW)

    def test_upload_form_exists(self):
        upload_form = self.browser.find_element_by_id('upload-form')

        self.assertIsNotNone(upload_form.find_element_by_name('file'))
        self.assertIsNotNone(upload_form.find_element_by_name('path'))
        self.assertIsNotNone(upload_form.find_element_by_id('upload-btn'))

    def test_edit_modals(self):
        """
        Test that edit modals correctly load the information of the selected
        row and that data entered into the edit form is submitted correctly.
        """
        TEST_TXT = 'THING'
        table_body = self.browser.find_elements_by_css_selector(TR_SELECTOR)
        data_rows = [row for row in table_body if len(
            row.find_elements_by_css_selector('td')) == DATA_CELLS_PER_ROW]

        selected_row = data_rows[0]

        edit_button = selected_row.find_element_by_css_selector(
            'button[data-target="#cert-modal"]')
        edit_button.click()

        modal = WebDriverWait(self.browser, 5).until(
            EC.visibility_of(self.browser.find_element_by_id('cert-modal')))
        form = modal.find_element_by_tag_name('form')

        # Modal is visible
        self.assertEqual(modal.get_attribute('style'), 'display: block;')

        # Compare form values to row data
        compare_edit_form_to_row(self, selected_row, modal)

        orig_cert_type = modal.find_element_by_id(
            'cert_type').get_attribute('value')
        set_data(form, 'cert_type', TEST_TXT)
        modal.find_element_by_css_selector(
            'button[data-dismiss="modal"].btn.btn-secondary').click()

        WebDriverWait(self.browser, 5).until(element_is_clicked(edit_button))
        WebDriverWait(self.browser, 5).until(EC.visibility_of(modal))

        # Value is changed in form and row data matches
        self.assertEqual(modal.find_element_by_id(
            'cert_type').get_attribute('value'), TEST_TXT)
        compare_edit_form_to_row(self, selected_row, modal)

        # Reset Cert Type
        set_data(form, 'cert_type', orig_cert_type)
        modal.find_element_by_css_selector(
            'button[data-dismiss="modal"].btn.btn-secondary').click()

        # Choose a new row and repeat test
        # new_row = data_rows[1]

        # edit_button = new_row.find_element_by_css_selector(
        #     'button[data-target="#cert-modal"]')

        # WebDriverWait(self.browser, 5).until(element_is_clicked(edit_button))
        # modal = WebDriverWait(self.browser, 5).until(
        #     EC.visibility_of(self.browser.find_element_by_id('cert-modal')))
        # form = modal.find_element_by_tag_name('form')

        # Modal is visible
        # self.assertEqual(modal.get_attribute('style'), 'display: block;')

        # Compare form values to row data
        # compare_edit_form_to_row(self, new_row, modal)

        # orig_cert_type = modal.find_element_by_id(
        #     'cert_type').get_attribute('value')

        # input_field = form.find_element_by_id('cert_type')
        # input_field.clear()
        # input_field.send_keys(TEST_TXT)
        # form.submit()

        # modal.find_element_by_css_selector(
        #     'button[data-dismiss="modal"].btn.btn-secondary').click()

        # WebDriverWait(self.browser, 5).until(element_is_clicked(edit_button))
        # WebDriverWait(self.browser, 5).until(EC.visibility_of(modal))

        # Value is changed in form and row data matches
        # self.assertEqual(modal.find_element_by_id(
        #     'cert_type').get_attribute('value'), TEST_TXT)
        # compare_edit_form_to_row(self, new_row, modal)

        # Reset Cert Type
        # set_data(form, 'cert_type', orig_cert_type)
        # modal.find_element_by_css_selector(
        #     'button[data-dismiss="modal"].btn.btn-secondary').click()

    def test_delete_buttons(self):
        pass

    def test_search_form(self):
        """
        Test that search form filters rows from table of teacher and staff BOE
        certifications.  Assumes DB has an entry with Cert-No BI-03-2010.
        """
        search_text = 'BI-03-2010'
        init_table_body = self.browser.find_elements_by_css_selector(
            TR_SELECTOR)
        self.browser.find_element_by_id('search-certs').send_keys(search_text)

        post_table_body = self.browser.find_elements_by_css_selector(
            TR_SELECTOR)

        # Search filtered out rows that don't match, reducing the number of
        # rows in the post_table_body
        self.assertLess(len(post_table_body), len(init_table_body))

        # Remainging rows all match search text
        for row in post_table_body:
            n_data = len(row.find_elements_by_css_selector('td'))
            if n_data == DATA_CELLS_PER_ROW:
                self.assertEqual(row.find_element_by_css_selector(
                    'th').find_element_by_tag_name('p').text, search_text)


def set_data(form, field_id, value):
    input_field = form.find_element_by_id(field_id)
    for i in range(100):
        input_field.send_keys(Keys.BACKSPACE)
    input_field.send_keys(value)
    form.submit()


def compare_edit_form_to_row(testCase, row, modal):
    row_data = row.find_elements_by_css_selector('p')

    cert_no_field = modal.find_element_by_id('cert_no')
    last_name_field = modal.find_element_by_id('last_name')
    first_name_field = modal.find_element_by_id('first_name')
    middle_name_field = modal.find_element_by_id('mi')
    cert_type_field = modal.find_element_by_id('cert_type')
    start_date_field = modal.find_element_by_id('start_date')
    expiry_date_field = modal.find_element_by_id('expiry_date')

    # Field defaults match row data
    cert_no = cert_no_field.get_attribute('value')
    last_name = last_name_field.get_attribute('value')
    first_name = first_name_field.get_attribute('value')
    middle_name = middle_name_field.get_attribute('value')
    cert_type = cert_type_field.get_attribute('value')
    start_date = start_date_field.get_attribute('value')
    expiry_date = expiry_date_field.get_attribute('value')

    testCase.assertEqual(row_data[0].text, cert_no)
    testCase.assertEqual(row_data[1].text, last_name)
    testCase.assertEqual(row_data[2].text, first_name + ' ' + middle_name)
    testCase.assertEqual(row_data[3].text, cert_type)
    testCase.assertEqual(row_data[4].text, start_date)
    testCase.assertEqual(row_data[5].text, expiry_date)


class element_is_clicked(object):
    """
    An expectation for checking that a WebElement is clickable
    """

    def __init__(self, element):
        self.element = element

    def __call__(self, driver):
        try:
            self.element.click()
            return self.element
        except WebDriverException:
            return False
