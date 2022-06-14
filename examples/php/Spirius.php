<?php

use GuzzleHttp\Client;

class Spirius
{
    protected $key = '<copied key from the account settings page>';

    protected $username = "username";

    protected $baseUrl = 'https://rest.spirius.com/v1';

    protected $sendEndpoint = '/sms/mt/send';

    protected $messageBody = [];

    protected $authVersion = "SpiriusSmsV1";

    public function createHeaders($signature, $timestamp)
    {
        return [
            "X-SMS-Timestamp" => $timestamp,
            "Authorization" => "{$this->authVersion} {$this->username}:{$signature}",
            "Content-Type" => "application/json",
        ];
    }

    public function createSignature($timestamp)
    {
        $bodyHash = sha1(utf8_encode(json_encode($this->messageBody)));

        $messageToSign = implode("\n", [
            $this->authVersion,
            $timestamp,
            "POST",
            $this->sendEndpoint,
            $bodyHash
        ]);

        $digest = hash_hmac(
            "sha256",
            utf8_encode($messageToSign),
            utf8_encode($this->key),
            true
        );

        return utf8_decode(base64_encode($digest));
    }

    public function performRequest($timestamp)
    {
        $signature = $this->createSignature($timestamp);
        $headers = $this->createHeaders($signature, $timestamp);

        // Guzzle (https://docs.guzzlephp.org/en/stable) is used to make the http request
        // https://docs.guzzlephp.org/en/stable/overview.html#installation
        $client = new Client();

        $response = $client->request(
            'POST',
            $this->baseUrl . $this->sendEndpoint,
            [
                'headers' => $headers,
                'json' => $this->messageBody,
                'connect_timeout' => 5
            ]
        );

        return json_decode($response->getBody(), true);
    }

    public function sendSms($data)
    {
        $timestamp = time();

        $this->messageBody = [
            'message' => $data['message'],
            'from' => $data['sender'],
            'to' => $data['recipient'],
        ];

        return $this->performRequest($timestamp);
    }
}

// Send an SMS
(new Spirius())->sendSms([
    'message' => 'test message',
    'recipient' => '<number>',
    'sender' => '<number>'
]);