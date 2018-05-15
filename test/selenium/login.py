"""
Provides a login method for functional tests to use
"""
from os import environ


def login(browser, ident, pwd, url):
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
