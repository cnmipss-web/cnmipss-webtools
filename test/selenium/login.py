"""
Provides a login method for functional tests to use
"""
from os import environ

DEFAULT_ID = environ.get('WT_ID')
DEFAULT_PWD = environ.get('WT_PASS')
DEFAULT_URL = 'http://localhost.com/webtools'


def login(browser, ident=DEFAULT_ID, pwd=DEFAULT_PWD, url=DEFAULT_URL):
    "Automatically login using Google OAuth"
    browser.get(url)
    browser.implicitly_wait(5)
    browser.find_element_by_id('oauth-button').click()
    browser.find_element_by_id(
        'identifierId').send_keys(ident)
    browser.find_element_by_id('identifierNext').click()
    browser.find_element_by_name(
        'password').send_keys(pwd)
    browser.execute_script(
        'document.getElementById("passwordNext").click()')
