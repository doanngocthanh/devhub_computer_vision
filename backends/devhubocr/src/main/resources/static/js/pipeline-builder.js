// Minimal pipeline-builder interactions (drag & drop skeleton)
(function(){
  function $(sel, root) { return (root||document).querySelector(sel); }
  function $all(sel, root) { return Array.from((root||document).querySelectorAll(sel)); }

  var stepList = $('#step-list');
  var canvas = $('#canvas');
  var resultArea = document.getElementById('resultArea');

  // make steps draggable
  $all('.step-item').forEach(function(it){
    it.draggable = true;
    it.addEventListener('dragstart', function(e){
      e.dataTransfer.setData('text/plain', it.getAttribute('data-bean'));
    });
  });

  canvas.addEventListener('dragover', function(e){ e.preventDefault(); });
  canvas.addEventListener('drop', function(e){
    e.preventDefault();
    var bean = e.dataTransfer.getData('text/plain');
    var el = document.createElement('div');
    el.className = 'card p-2 mb-2';
    el.setAttribute('data-bean', bean);
    el.innerHTML = '<div><strong>'+bean+'</strong></div><div class="small text-muted">id: step'+Date.now()+'</div>';
    canvas.querySelector('#canvas-inner').appendChild(el);
    // open parameter editor on click
    el.addEventListener('click', function(){ openParamEditor(bean, el); });
  });

  document.getElementById('exportBtn').addEventListener('click', function(){
    var steps = [];
    $all('#canvas .card').forEach(function(c, idx){
      steps.push({ id: 'step'+(idx+1), bean: c.getAttribute('data-bean'), input: {}, outputKey: null });
    });
    var json = { name: 'New Pipeline', steps: steps };
    resultArea.textContent = JSON.stringify(json, null, 2);
  });

  document.getElementById('saveBtn').addEventListener('click', function(){
    try {
      var payload = JSON.parse(resultArea.textContent || '{}');
      fetch('/A0/WLA0_0100/save', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload)})
        .then(r=>r.json()).then(j=>{ alert('Saved: '+JSON.stringify(j)); });
    } catch(e){ alert('Export JSON first'); }
  });

  document.getElementById('runBtn').addEventListener('click', function(){
    try {
      var payload = JSON.parse(resultArea.textContent || '{}');
      fetch('/A0/WLA0_0100/run', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload)})
        .then(r=>r.json()).then(j=>{ renderResult(j); });
    } catch(e){ alert('Export JSON first'); }
  });

  // Run with uploaded file
  var runWithFileBtn = document.getElementById('runWithFileBtn');
  if (runWithFileBtn) {
    runWithFileBtn.addEventListener('click', function(){
      try {
        var payload = JSON.parse(resultArea.textContent || '{}');
      } catch(e){ alert('Export JSON first'); return; }
      var input = document.getElementById('pipelineFileInput');
      if (!input || !input.files || input.files.length === 0) { alert('Choose a file first'); return; }
      var fd = new FormData();
      fd.append('file', input.files[0]);
      fd.append('pipeline', JSON.stringify(payload));
      fetch('/A0/WLA0_0100/run-with-file', { method: 'POST', body: fd })
        .then(r=>r.json()).then(j=>{ renderResult(j); })
        .catch(err=>{ alert('Run failed: '+err); });
    });
  }

  function renderResult(j) {
    try {
      // if success and has result with values that are URLs to /A0/WLA0_0100/output, show previews
      var pretty = JSON.stringify(j, null, 2);
      var html = '<div style="white-space:pre-wrap; font-family:monospace; background:#f8f9fa; padding:0.5rem;">' + escapeHtml(pretty) + '</div>';
      // try to extract any output URLs
      var urls = JSON.stringify(j).match(/"(\/A0\/WLA0_0100\/output\?name=[^"\\]+)"/g) || [];
      var thumbs = '';
      urls.forEach(function(m){
        var u = m.replace(/^"|"$/g,'');
        var decoded = u;
        // if image extension, render <img>
        if (decoded.match(/\.(png|jpg|jpeg|gif|bmp)$/i)) {
          thumbs += '<div style="display:inline-block;margin:4px;text-align:center;">';
          thumbs += '<a href="'+decoded+'" target="_blank"><img src="'+decoded+'" style="max-width:160px;max-height:120px;border:1px solid #ddd"/></a>';
          thumbs += '<div><a href="'+decoded+'" target="_blank">Download</a></div></div>';
        } else {
          thumbs += '<div><a href="'+decoded+'" target="_blank">View output</a></div>';
        }
      });

      resultArea.innerHTML = '';
      if (thumbs) {
        var container = document.createElement('div');
        container.innerHTML = thumbs + '<hr/>' + html;
        resultArea.appendChild(container);
      } else {
        resultArea.textContent = pretty;
      }
    } catch (e) {
      resultArea.textContent = JSON.stringify(j, null, 2);
    }
  }

  function escapeHtml(s) { return s.replace(/[&<>\"]/g, function(c){ return {'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[c]; }); }

  // --- Step parameter editor (simple modal) ---
  function openParamEditor(bean, element) {
    var modal = document.getElementById('stepParamModal');
    var body = document.getElementById('modalBody');
    var title = document.getElementById('modalTitle');
    var result = document.getElementById('modalResult');
    title.textContent = bean;
    result.textContent = '';
    // fetch step metadata
    fetch('/A0/WLA0_0100/steps').then(r=>r.json()).then(list=>{
      var meta = list.find(s=>s.bean === bean || s.bean === bean + '');
      body.innerHTML = '';
      if (!meta) { body.innerHTML = '<div class="text-muted">No metadata for '+bean+'</div>'; modal.style.display='block'; return; }
      // build input fields
      var inputs = meta.inputs || [];
      inputs.forEach(function(p){
        var id = 'param-'+p.name;
        var row = document.createElement('div');
        row.className = 'mb-2';
        var label = document.createElement('label'); label.className='form-label'; label.textContent = p.name + (p.required? ' *':'') + ' ('+ (p.type||'string') +')';
        var inp = document.createElement('input'); inp.className='form-control'; inp.id = id; inp.value = p.defaultValue || '';
        row.appendChild(label); row.appendChild(inp);
        body.appendChild(row);
      });
      modal.style.display='block';
      // wire run button
      document.getElementById('modalRun').onclick = function(){
        var input = {};
        inputs.forEach(function(p){ input[p.name] = document.getElementById('param-'+p.name).value; });
        document.getElementById('modalResult').textContent = 'Running...';
        fetch('/A0/WLA0_0100/run-step', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ bean: bean, input: input })})
          .then(r=>r.json()).then(j=>{ document.getElementById('modalResult').textContent = JSON.stringify(j, null, 2); })
          .catch(err=>{ document.getElementById('modalResult').textContent = 'Error: '+err; });
      };
      document.getElementById('modalClose').onclick = function(){ modal.style.display='none'; };
    });
  }
})();
