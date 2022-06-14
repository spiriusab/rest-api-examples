<?php

require 'vendor/autoload.php';
use GuzzleHttp\Client;

class Spirius
{
    protected $key = null;

    protected $username = null;

    protected $baseUrl = 'https://rest.spirius.com/v1';

    protected $sendEndpoint = '/sms/mt/send';

    protected $messageBody = [];

    protected $authVersion = 'SpiriusSmsV1';

    public function __construct($sharedKey, $username) {
        $this->key = $sharedKey;
        $this->username = $username;
    }

    public function createHeaders($signature, $timestamp): array
    {
        return [
            'X-SMS-Timestamp' => $timestamp,
            'Authorization' => "{$this->authVersion} {$this->username}:{$signature}",
            'Content-Type' => 'application/json',
        ];
    }

    public function createSignature($timestamp): string
    {
        $bodyHash = sha1(utf8_encode(json_encode($this->messageBody)));

        $messageToSign = implode('\n', [
            $this->authVersion,
            $timestamp,
            'POST',
            $this->sendEndpoint,
            $bodyHash,
        ]);

        $digest = hash_hmac(
            'sha256',
            utf8_encode($messageToSign),
            utf8_encode($this->key),
            true,
        );

        return utf8_decode(base64_encode($digest));
    }

    public function performRequest($timestamp): array
    {
        $signature = $this->createSignature($timestamp);
        $headers = $this->createHeaders($signature, $timestamp);

        // Guzzle (https://docs.guzzlephp.org/en/stable) is used to make the HTTP request
        // https://docs.guzzlephp.org/en/stable/overview.html#installation
        $client = new Client();

        $response = $client->request(
            'POST',
            $this->baseUrl . $this->sendEndpoint,
            [
                'headers' => $headers,
                'json' => $this->messageBody,
                'connect_timeout' => 5,
            ]
        );

        return json_decode($response->getBody(), true);
    }

    public function sendSms($data): array
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


$spirius = new Spirius(
    // Key is available on the account page on https://portal.spirius.com
    '78701a30f3f83437df6284ced6fc9ba58ca6a31c8031df3e8cb7a17eca7b91ed',
    'test',
);

$spirius->sendSms([
    'message' => 'Hello world!',
    'recipient' => '+46123456789',
    'sender' => 'SPIRIUS',
]);