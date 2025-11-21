// Minimal pipeline-builder interactions (drag & drop skeleton)
(function(){
  function $(sel, root) { return (root||document).querySelector(sel); }
  function $all(sel, root) { return Array.from((root||document).querySelectorAll(sel)); }

  var stepList = $('#step-list');
  var canvas = $('#canvas');
  var resultArea = document.getElementById('resultArea');
  var stepMeta = {};

  // fetch available steps from backend and render
  fetch('/A0/WLA0_0100/steps').then(r=>r.json()).then(list=>{
    var inner = document.getElementById('step-list-inner');
    if (!inner) return;
    list.forEach(function(s){
      var it = document.createElement('div');
      it.className = 'step-item';
      it.setAttribute('data-bean', s.bean);
      it.setAttribute('data-name', s.name || s.bean);
      it.textContent = (s.name || s.bean) + ' (' + ((s.inputs||[]).map(i=>i.name+':'+(i.type||'string')).join(',')||'') + ')';
      inner.appendChild(it);
      stepMeta[s.bean] = s;
      // make draggable
      it.draggable = true;
      it.addEventListener('dragstart', function(e){
        e.dataTransfer.setData('text/plain', s.bean);
      });
    });
  }).catch(err=>{
    console.warn('Failed to load steps', err);
  });
  // end fetch steps

  canvas.addEventListener('dragover', function(e){ e.preventDefault(); });
  canvas.addEventListener('drop', function(e){
    e.preventDefault();
    var bean = e.dataTransfer.getData('text/plain');
    var meta = stepMeta[bean] || null;
    var inner = canvas.querySelector('#canvas-inner');
    var prev = inner.lastElementChild;
    var el = document.createElement('div');
    el.className = 'card p-2 mb-2';
    el.setAttribute('data-bean', bean);
    el.dataset.inputs = JSON.stringify((meta && meta.inputs) || []);
    el.dataset.outputs = JSON.stringify((meta && meta.outputs) || []);
    var id = 'step'+Date.now();
    el.innerHTML = '<div><strong>'+ (meta && meta.name ? meta.name : bean) +'</strong></div><div class="small text-muted">id: '+id+'</div>';
    // compatibility check with previous node (simple: compare first output type -> first input type)
    if (prev) {
      try {
        var prevOuts = JSON.parse(prev.dataset.outputs || '[]');
        var nextIns = (meta && meta.inputs) || [];
        if (prevOuts.length > 0 && nextIns.length > 0) {
          var prevType = (prevOuts[0].type || '').toLowerCase();
          var nextType = (nextIns[0].type || '').toLowerCase();
          if (prevType && nextType && prevType !== nextType) {
            el.classList.add('border','border-danger');
            var warn = document.createElement('div'); warn.className='text-danger small'; warn.textContent='Type mismatch: '+prevType+' -> '+nextType;
            el.appendChild(warn);
          } else {
            el.classList.add('border','border-success');
          }
        }
      } catch (e) { /* ignore parse errors */ }
    }
    inner.appendChild(el);
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

  var currentEditingElement = null;

  // --- Step parameter editor (simple modal) ---
  function openParamEditor(bean, element) {
    var modal = document.getElementById('stepParamModal');
    var body = document.getElementById('modalBody');
    var title = document.getElementById('modalTitle');
    var result = document.getElementById('modalResult');
    title.textContent = bean;
    result.textContent = '';
    currentEditingElement = element;
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
        // save input to card dataset for later export/validation
        try { if (currentEditingElement) currentEditingElement.dataset.input = JSON.stringify(input); } catch(e){}
        document.getElementById('modalResult').textContent = 'Running...';
        fetch('/A0/WLA0_0100/run-step', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ bean: bean, input: input })})
          .then(r=>r.json()).then(j=>{ document.getElementById('modalResult').textContent = JSON.stringify(j, null, 2); })
          .catch(err=>{ document.getElementById('modalResult').textContent = 'Error: '+err; });
      };
      document.getElementById('modalClose').onclick = function(){ modal.style.display='none'; };
    });
  }

  // Validate pipeline button
  var validateBtn = document.getElementById('validateBtn');
  if (validateBtn) {
    validateBtn.addEventListener('click', function(){
      // try to use exported JSON first
      var payload = null;
      try { payload = JSON.parse(resultArea.textContent || '{}'); } catch(e){ payload = null; }
      if (!payload || !payload.steps) {
        // build from canvas
        var steps = [];
        $all('#canvas .card').forEach(function(c, idx){
          var input = {};
          try { input = JSON.parse(c.dataset.input || '{}'); } catch(e) { input = {}; }
          steps.push({ id: c.querySelector('.small') ? c.querySelector('.small').textContent : 'step'+(idx+1), bean: c.getAttribute('data-bean'), input: input, outputKey: c.dataset.outputKey || null });
        });
        payload = { name: 'Pipeline', steps: steps };
      }

      // post to validator
      fetch('/A0/WLA0_0100/validate-pipeline', { method:'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ pipeline: payload })})
        .then(r=>r.json()).then(j=>{
          var va = document.getElementById('validationArea');
          va.innerHTML = '';
          if (!j) { va.textContent = 'No response'; return; }
          if (j.ok) {
            va.innerHTML = '<div class="alert alert-success">Pipeline OK</div>';
            // clear any previous highlights
            $all('#canvas .card').forEach(function(c){ c.classList.remove('border-danger'); c.classList.remove('border-warning'); });
          } else {
            var issues = j.issues || [];
            var ul = document.createElement('ul');
            ul.className = 'list-group';
            issues.forEach(function(it){
              var li = document.createElement('li'); li.className='list-group-item list-group-item-danger'; li.textContent = it; ul.appendChild(li);
              // try to highlight step by extracting step bean name from message
              var m = it.match(/step '([^']+)'/);
              if (m) {
                var bean = m[1];
                $all('#canvas .card').forEach(function(c){ if (c.getAttribute('data-bean') === bean) { c.classList.add('border-danger'); } });
              }
            });
            va.appendChild(ul);
          }
        }).catch(err=>{ alert('Validation failed: '+err); });
    });
  }
})();
