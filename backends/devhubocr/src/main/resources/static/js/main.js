document.addEventListener('DOMContentLoaded', function(){
    // Helpers
    function escapeHtml(s){ 
        return String(s||'').replace(/[&<>"']/g, function(m){
            return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[m]; 
        }); 
    }

    function createToastContainer(){
        var container = document.getElementById('notification-toasts');
        if (container) return container;
        container = document.createElement('div');
        container.id = 'notification-toasts';
        container.style.position = 'fixed';
        container.style.top = '1rem';
        container.style.right = '1rem';
        container.style.zIndex = 2147483647;
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.gap = '0.5rem';
        document.body.appendChild(container);
        return container;
    }

    function showToast(title, message){
        var container = createToastContainer();
        var t = document.createElement('div');
        t.className = 'toast show';
        t.setAttribute('role', 'alert');
        t.innerHTML = '<div class="toast-header">' +
                      '<strong class="me-auto">' + escapeHtml(title) + '</strong>' +
                      '<button type="button" class="btn-close" data-bs-dismiss="toast"></button>' +
                      '</div>' +
                      '<div class="toast-body">' + escapeHtml(message) + '</div>';
        container.appendChild(t);
        
        // Auto remove after 5 seconds
        setTimeout(function(){ 
            if (t.parentNode) t.parentNode.removeChild(t); 
        }, 5000);
        
        // Close button handler
        var closeBtn = t.querySelector('.btn-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', function(){
                if (t.parentNode) t.parentNode.removeChild(t);
            });
        }
    }

    function updateBadge(bellToggle, increment){
        var badge = bellToggle.querySelector('.badge');
        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'badge bg-red';
            bellToggle.appendChild(badge);
        }
        var current = parseInt(badge.textContent) || 0;
        var newCount = current + increment;
        badge.textContent = newCount > 0 ? newCount : '';
        if (newCount <= 0) badge.style.display = 'none';
        else badge.style.display = '';
    }

    function prependToDropdown(dropdown, data){
        try {
            var listGroup = dropdown.querySelector('.list-group.list-group-flush');
            if (!listGroup) return;
            
            var title = escapeHtml(data.title || 'Thông báo');
            var msg = escapeHtml(data.message || '');
            var delivery = data.delivery_id || '';
            
            var item = document.createElement('div');
            item.className = 'list-group-item';
            item.innerHTML = '<div class="row align-items-center">' +
                           '<div class="col-auto"><span class="status-dot status-dot-animated bg-red d-block"></span></div>' +
                           '<div class="col text-truncate">' +
                           '<a href="/AA/A0/AAA0_0104/" class="text-body d-block">'+title+'</a>' +
                           '<div class="d-block text-secondary text-truncate mt-n1">'+msg+'</div>' +
                           '</div>' +
                           '<div class="col-auto">' +
                           '<a href="/AA/A0/AAA0_0104/mark/read?deliveryId='+delivery+'" class="list-group-item-actions">Đánh dấu</a>' +
                           '</div>' +
                           '</div>';
            
            listGroup.insertBefore(item, listGroup.firstChild);
        } catch(e){ 
            console.error('prependToDropdown error', e); 
        }
    }

    // Main wiring
    var bellToggle = document.querySelector('a[aria-label="Show notifications"]');
    var dropdown = bellToggle ? bellToggle.closest('.nav-item.dropdown') : null;

    // If notification anchor exists, wire only the bell-specific behavior.
    if (bellToggle && dropdown) {
        // SSE realtime
        try {
            var streamUid = bellToggle.getAttribute('data-user-id');
            if (streamUid && streamUid !== '') {
                try {
                    var es = new EventSource('/AA/A0/AAA0_0104/stream?userId=' + encodeURIComponent(streamUid));
                    es.addEventListener('notification', function(e){
                        try {
                            var data = JSON.parse(e.data);
                            // show toast
                            showToast(data.title || 'Thông báo', data.message || '');
                            // update badge
                            updateBadge(bellToggle, 1);
                            // optionally prepend to dropdown
                            prependToDropdown(dropdown, data);
                        } catch(err){ 
                            console.error('sse notification parse', err); 
                        }
                    });
                    es.addEventListener('connected', function(e){ /*noop*/ });
                    es.onerror = function(ev){ 
                        console.warn('EventSource error', ev); 
                    };
                } catch(ex) { 
                    console.warn('EventSource setup failed', ex); 
                }
            }
        } catch(e){ 
            console.warn('SSE open failed', e); 
        }

        // populate list when dropdown shown
        try {
            dropdown.addEventListener('shown.bs.dropdown', async function(){
                try {
                    var uid = bellToggle.getAttribute('data-user-id');
                    var url = '/AA/A0/AAA0_0104/notify';
                    if (uid && uid !== '') url += '?userId=' + encodeURIComponent(uid);
                    var res = await fetch(url, {credentials: 'same-origin'});
                    if (!res.ok) return;
                    var items = await res.json();
                    var listGroup = dropdown.querySelector('.list-group.list-group-flush');
                    if (!listGroup) return;
                    
                    if (!items || items.length === 0) {
                        listGroup.innerHTML = '<div class="list-group-item"><div class="text-center text-secondary">Không có thông báo mới.</div></div>';
                        return;
                    }
                    
                    var html = '';
                    for (var i=0; i<Math.min(items.length,5); i++){
                        var n = items[i];
                        var isRead = (n.is_read == 1 || n.is_read === true);
                        var title = escapeHtml(n.title || '');
                        var msg = escapeHtml(n.message || '');
                        var delivery = n.delivery_id || '';
                        var dotClass = isRead ? 'status-dot d-block' : 'status-dot status-dot-animated bg-red d-block';
                        html += '<div class="list-group-item">' +
                               '<div class="row align-items-center">' +
                               '<div class="col-auto"><span class="'+dotClass+'"></span></div>' +
                               '<div class="col text-truncate">' +
                               '<a href="/AA/A0/AAA0_0104/" class="text-body d-block">'+title+'</a>' +
                               '<div class="d-block text-secondary text-truncate mt-n1">'+msg+'</div>' +
                               '</div>' +
                               '<div class="col-auto">' +
                               '<a href="/AA/A0/AAA0_0104/mark/read?deliveryId='+delivery+'" class="list-group-item-actions">Đánh dấu</a>' +
                               '</div>' +
                               '</div></div>';
                    }
                    listGroup.innerHTML = html;
                } catch(e){ 
                    console.error('notify fetch error', e); 
                }
            });
        } catch(e){ 
            /* ignore */ 
        }
    }
});

// Signal that main.js has finished wiring (useful for pages that wait for this script)
try { 
    window.DEVHUB_MAIN_LOADED = true; 
} catch (e) { 
    /* ignore */ 
}