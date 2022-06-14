import base64
import hmac
import hashlib
import json
import time
from typing import Dict

import requests
from requests import Response


class SpiriusHttpClient:
    """
    HTTP client for sending SMS to, and receiving SMS from, Spirius SMS Gateway via its REST API. The HMAC auth method
    is used, and signatures for this are generated automatically.

    The requests library is used for performing the HTTP requests. All public methods of this class return a Response
    object from the requests lib.
    """
    __BASE_URL = "https://rest.spirius.com/v1"
    __ENCODING = "utf8"
    __AUTH_VERSION = "SpiriusSmsV1"
    __REQUEST_TIMEOUT_SECONDS = 5

    def __init__(self, shared_key: str, username: str):
        self.__shared_key = shared_key
        self.__username = username

    def send_sms(self, to_str: str, from_str: str, message: str) -> Response:
        path = "/sms/mt/send"
        verb = "POST"
        request_body = {
            "message": message,
            "to": to_str,
            "from": from_str,
        }

        return self.__perform_request(verb, path, request_body)

    def get_message_status(self, transaction_id: str) -> Response:
        path = f"/sms/mo/status/{transaction_id}"
        verb = "GET"

        return self.__perform_request(verb, path)

    def get_mo_message_list(self) -> Response:
        path = f"/sms/mo"
        verb = "GET"

        return self.__perform_request(verb, path)

    def get_mo_message(self, transaction_id: str) -> Response:
        path = f"/sms/mo/{transaction_id}"
        verb = "GET"

        return self.__perform_request(verb, path)

    def pop_mo_message(self, transaction_id: str) -> Response:
        path = f"/sms/mo/{transaction_id}"
        verb = "DELETE"

        return self.__perform_request(verb, path)

    def pop_next_message(self) -> Response:
        path = f"/sms/mo/next"
        verb = "DELETE"

        return self.__perform_request(verb, path)

    def __perform_request(self, verb: str, path: str, body: Dict = None) -> Response:
        url = f"{self.__BASE_URL}{path}"
        timestamp = self.__create_unix_timestamp()
        signature = self.__create_signature(path, timestamp, verb, body)
        headers = self.__create_headers(signature, timestamp)

        return requests.request(
            method=verb,
            url=url,
            headers=headers,
            json=body,
            timeout=self.__REQUEST_TIMEOUT_SECONDS,
        )

    @staticmethod
    def __create_unix_timestamp() -> str:
        return str(int(time.time()))

    def __create_signature(self, path: str, timestamp: str, verb: str, body: Dict = None) -> str:
        if body:
            request_body_json = json.dumps(body)
            body_hash = hashlib.sha1(request_body_json.encode(self.__ENCODING))
        else:
            body_hash = hashlib.sha1("".encode(self.__ENCODING))

        msg_to_sign = "\n".join([
            self.__AUTH_VERSION,
            timestamp,
            verb,
            path,
            body_hash.hexdigest(),
        ])

        digest = hmac.new(
            key=self.__shared_key.encode(self.__ENCODING),
            msg=msg_to_sign.encode(self.__ENCODING),
            digestmod=hashlib.sha256,
        ).digest()

        return base64.b64encode(digest).decode(self.__ENCODING)

    def __create_headers(self, signature, timestamp) -> Dict[str, str]:
        return {
            "X-SMS-Timestamp": timestamp,
            "Authorization": f"{self.__AUTH_VERSION} {self.__username}:{signature}",
            "Content-Type": "application/json",
        }


if __name__ == "__main__":
    client = SpiriusHttpClient(
        # Key is available on the account page on https://portal.spirius.com
        shared_key="78701a30f3f83437df6284ced6fc9ba58ca6a31c8031df3e8cb7a17eca7b91ed",
        username="test"
    )

    response = client.send_sms(
        to_str="+46123456789",
        from_str="SPIRIUS",
        message="Hello world!"
    )
    print(response.json())
