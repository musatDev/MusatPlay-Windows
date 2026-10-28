(() => {
    if (window.top !== window || location.origin !== 'https://music.youtube.com') return null;
    if (!window.__musatKeys) {
        window.__musatKeys = true;
        document.addEventListener('keydown', event => {
            if (event.ctrlKey && event.code === 'KeyQ') { event.preventDefault(); window.__musatQuit = true; }
        });
    }
    const metadata = navigator.mediaSession?.metadata;
    const video = document.querySelector('video');
    const bar = document.querySelector('ytmusic-player-bar');
    const repeat = (bar?.getAttribute('repeat-mode') || 'NONE').toUpperCase();
    const title = metadata?.title || '';
    const artist = metadata?.artist || '';
    const album = metadata?.album || '';
    const id = new URL(location.href).searchParams.get('v');
    return {url: location.href, pageTitle: document.title || 'MusatPlay', quit: Boolean(window.__musatQuit), state: {
        trackId: title ? (id || JSON.stringify([title, artist, album])) : '', title, artist,
        artwork: metadata?.artwork?.at(-1)?.src || '',
        playing: Boolean(video && !video.paused && !video.ended),
        shuffle: Boolean(bar?.hasAttribute('shuffle-on')),
        repeat: ['ALL', 'ONE'].includes(repeat) ? repeat : 'NONE'
    }};
})();
