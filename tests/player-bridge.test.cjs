const test = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const {runInNewContext} = require('node:vm');
const source = readFileSync(join(__dirname, '../src/main/resources/player-bridge.js'), 'utf8');
function fixture(origin = 'https://music.youtube.com') {
    const listeners = {};
    const video = {paused: false, ended: false};
    const bar = {getAttribute: () => 'ONE', hasAttribute: () => true};
    const context = {location: {origin, href: origin + '/watch?v=abc'}, URL,
        navigator: {mediaSession: {metadata: {title: 'Track', artist: 'Artist', artwork: [{src:'https://i.ytimg.com/a'}]}}},
        document: {title: 'Music', querySelector: selector => selector === 'video' ? video : bar,
            addEventListener: (type, callback) => { listeners[type] = callback; }}, window: {}};
    context.window.top = context.window;
    return {context, listeners, video, read: () => runInNewContext(source, context)};
}
test('reads playback, artwork and modes without a persistent page timer', () => {
    const f = fixture(), result = f.read();
    assert.equal(result.state.trackId, 'abc');
    assert.equal(result.state.repeat, 'ONE');
    assert.equal(result.state.playing, true);
    assert.equal(result.state.shuffle, true);
    f.video.paused = true;
    assert.equal(f.read().state.playing, false);
});
test('clears removed metadata and captures late artwork', () => {
    const f = fixture();
    f.context.navigator.mediaSession.metadata.artwork.push({src:'https://i.ytimg.com/b'});
    assert.equal(f.read().state.artwork, 'https://i.ytimg.com/b');
    f.context.navigator.mediaSession.metadata = null;
    assert.equal(f.read().state.title, '');
    assert.equal(f.read().state.trackId, '');
});
test('rejects other origins, lookalikes and subframes', () => {
    assert.equal(fixture('https://example.com').read(), null);
    assert.equal(fixture('https://music.youtube.com.evil.com').read(), null);
    const f = fixture(); f.context.window.top = {};
    assert.equal(f.read(), null);
});
test('keyboard handler is installed once and reports Ctrl+Q', () => {
    const f = fixture(); f.read();
    const handler = f.listeners.keydown; f.read();
    assert.equal(f.listeners.keydown, handler);
    let prevented = false;
    handler({ctrlKey:true, code:'KeyQ', preventDefault: () => { prevented = true; }});
    assert.equal(f.read().quit, true);
    assert.equal(prevented, true);
});
