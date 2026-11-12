"""Exercise either deployment over HTTP; uses only Python's standard library.

Creates a temporary account and removes its post/account before returning.
Pass --check-ai only when provider calls and their cost are intended.
"""
import argparse
from http.client import HTTPException
import json
import re
import secrets
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:3000')
    parser.add_argument('--wait-seconds', type=int, default=180)
    parser.add_argument('--check-ai', action='store_true')
    args = parser.parse_args()
    base = args.base_url.rstrip('/')
    token = None

    def request(method, path, data=None, expected=200):
        headers = {'Accept': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        body = None
        if data is not None:
            headers['Content-Type'] = 'application/json'
            body = json.dumps(data).encode()
        try:
            response = urlopen(Request(base + path, data=body, headers=headers, method=method), timeout=60)
        except HTTPError as error:
            response = error
        with response:
            raw = response.read().decode()
            if response.status != expected:
                # Never print response bodies: they may contain tokens or personal data.
                raise RuntimeError(f'{method} {path}: expected {expected}, got {response.status}')
            return json.loads(raw) if raw and 'application/json' in response.headers.get('Content-Type', '') else raw

    deadline = time.monotonic() + args.wait_seconds
    while True:
        try:
            if request('GET', '/healthz') == {'status': 'ok'}:
                break
        except (OSError, HTTPException, RuntimeError):
            pass
        if time.monotonic() >= deadline:
            raise RuntimeError('Deployment did not become ready before the deadline')
        time.sleep(3)

    for path in ('/', '/login'):
        page = request('GET', path)
        assert isinstance(page, str) and '<div id="root">' in page, f'{path}: missing React document'
        asset = re.search(r'src="(/assets/[^\"]+\.js)"', page)
        assert asset, f'{path}: missing bundled JavaScript'
        assert 'text/html' not in str(request('GET', asset.group(1)))[:200], 'JavaScript asset resolved to HTML'
    request('GET', '/api/posts', expected=401)

    user_id = post_id = None
    try:
        username = 'smoke_' + secrets.token_hex(8)
        password = secrets.token_urlsafe(24)
        auth = request('POST', '/api/auth/signup', {
            'username': username, 'email': username + '@example.invalid', 'password': password,
        })
        token = auth['accessToken']
        user_id = request('GET', '/api/auth/me')['id']
        token = request('POST', '/api/auth/login', {'username': username, 'password': password})['accessToken']
        post = request('POST', '/api/posts', {'title': 'Cobalt deployment probe', 'contents': 'The Cobalt launch is on Tuesday.'})
        post_id = post['id']
        assert any(item['id'] == post_id for item in request('GET', '/api/posts')['content'])
        updated = request('PUT', f'/api/posts/{post_id}', {
            'title': 'Cobalt deployment probe', 'contents': 'The Cobalt launch is on Thursday.',
        })
        assert 'Thursday' in updated['contents']
        answer = request('POST', '/api/assistant', {'question': 'When is the Cobalt launch?'})
        assert any(source['postId'] == post_id and 'Thursday' in source['excerpt'] for source in answer['sources'])
        assert 'Thursday' in answer['answer']
        assert answer['mode'] == ('rag' if args.check_ai else 'search'), 'Unexpected assistant mode'
        request('DELETE', f'/api/posts/{post_id}', expected=204)
        post_id = None
        assert not request('GET', '/api/posts')['content']
        print(f'PASS {base}: frontend, login routing, auth, PostgreSQL CRUD and assistant citations')
    finally:
        if post_id is not None:
            request('DELETE', f'/api/posts/{post_id}', expected=204)
        if user_id is not None:
            request('DELETE', f'/api/users/{user_id}', expected=204)


if __name__ == '__main__':
    main()
