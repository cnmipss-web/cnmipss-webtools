from os import environ
import unittest
from re import match
from urllib.parse import urljoin
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions
from selenium.common.exceptions import NoSuchElementException

url_root = 'https://www.cnmipss.org'


class LoginTests(unittest.TestCase):
    "Functional tests for cnmipss.org/webtools application login"

    def setUp(self):
        chrome_options = Options()
        # chrome_options.add_argument('--headless')
        self.browser = webdriver.Chrome(chrome_options=chrome_options)
        self.browser.get('http://cnmipss.org/webtools')

    def tearDown(self):
        self.browser.close()

    def test_page_title_exists(self):
        "Test page title exists"
        self.assertEqual('CNMI PSS Webtools', self.browser.title)

    def test_login_button_exists(self):
        "Test that login page is rendered and exists"
        login_form = self.browser.find_element_by_id('login-form')
        login_button = self.browser.find_element_by_id('oauth-button')

        form_action_url = urljoin(url_root, '/webtools/oauth/oauth-init')

        self.assertIsNotNone(login_form)
        self.assertIsNotNone(login_button)
        self.assertEqual(form_action_url,
                         login_form.get_attribute('action'))
        self.assertEqual('get', login_form.get_attribute('method'))
        self.assertEqual('submit', login_button.get_attribute('type'))

    def test_oauth_workflow(self):
        "Test Google Oauth Workflow for Webtools"
        self.browser.implicitly_wait(5)
        self.browser.find_element_by_id('oauth-button').click()
        self.browser.find_element_by_id(
            'identifierId').send_keys(environ.get('WT_ID'))
        self.browser.find_element_by_id('identifierNext').click()
        self.browser.find_element_by_name(
            'password').send_keys(environ.get('WT_PASS'))
        self.browser.execute_script(
            'document.getElementById("passwordNext").click()')

        try:
            logout_link = self.browser.find_element_by_link_text('Logout')
            self.assertTrue(logout_link is not None)
        except NoSuchElementException as error:
            self.assertIsNone(error)
