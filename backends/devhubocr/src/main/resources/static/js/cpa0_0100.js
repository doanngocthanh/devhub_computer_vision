document.addEventListener('DOMContentLoaded', function() {
  const form = document.getElementById('ocrForm');
  const result = document.getElementById('result');
  const loadCanvasBtn = document.getElementById('loadCanvasBtn');
  const ocrSelectionBtn = document.getElementById('ocrSelectionBtn');
  const canvas = document.getElementById('imageCanvas');
  const selectionInfo = document.getElementById('selectionInfo');
  let ctx = null;
  let img = null;
  let scale = 1;
  let drag = false;
  let startX = 0, startY = 0, curX = 0, curY = 0;
  let backendRawPath = null;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    result.textContent = 'Uploading...';
    const fileInput = document.getElementById('file');
    if (!fileInput.files || fileInput.files.length === 0) {
      result.textContent = 'Please choose a file.';
      return;
    }
    const lang = document.getElementById('lang').value || '';
    const fd = new FormData();
    fd.append('file', fileInput.files[0]);
    fd.append('lang', lang);
    try {
      const res = await fetch('/CP/A0/CPA0_0100/upload-and-ocr', {
        method: 'POST',
        body: fd
      });
      const data = await res.json();
      if (data.ok) {
        result.textContent = data.text || '(no text)';
        // store raw path so bbox OCR can reference the same file
        backendRawPath = data.rawPath || null;
      } else {
        result.textContent = 'Error: ' + (data.error || 'unknown');
      }
    } catch (err) {
      result.textContent = 'Request failed: ' + err;
    }
  });

  // load selected file into canvas for drawing selection
  loadCanvasBtn.addEventListener('click', async () => {
    const fileInput = document.getElementById('file');
    if (!fileInput.files || fileInput.files.length === 0) {
      result.textContent = 'Please choose a file first.';
      return;
    }
    const file = fileInput.files[0];
    const url = URL.createObjectURL(file);
    img = new Image();
    img.onload = () => {
      // constrain width to 1000px for display
      const maxW = 1000;
      const dispW = Math.min(img.naturalWidth, maxW);
      scale = dispW / img.naturalWidth;
      canvas.width = dispW;
      canvas.height = Math.round(img.naturalHeight * scale);
      ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      selectionInfo.textContent = 'Drag on the image to draw a box.';
    };
    img.src = url;
  });

  // mouse handling for rectangle selection
  canvas && canvas.addEventListener('mousedown', (e) => {
    if (!img) return;
    drag = true;
    const r = canvas.getBoundingClientRect();
    startX = e.clientX - r.left;
    startY = e.clientY - r.top;
  });
  canvas && canvas.addEventListener('mousemove', (e) => {
    if (!img) return;
    if (!drag) return;
    const r = canvas.getBoundingClientRect();
    curX = e.clientX - r.left;
    curY = e.clientY - r.top;
    // redraw
    ctx.clearRect(0,0,canvas.width,canvas.height);
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = 'red';
    ctx.lineWidth = 2;
    ctx.strokeRect(startX, startY, curX - startX, curY - startY);
  });
  canvas && canvas.addEventListener('mouseup', (e) => {
    if (!img) return;
    if (!drag) return;
    drag = false;
    const r = canvas.getBoundingClientRect();
    curX = e.clientX - r.left;
    curY = e.clientY - r.top;
    const x = Math.round(Math.min(startX, curX));
    const y = Math.round(Math.min(startY, curY));
    const w = Math.round(Math.abs(curX - startX));
    const h = Math.round(Math.abs(curY - startY));
    selectionInfo.textContent = `Selection: x=${x}, y=${y}, w=${w}, h=${h}`;
    // keep last selection in dataset
    canvas.dataset.sel = JSON.stringify({x,y,w,h});
  });

  // OCR the selected bbox by posting to server
  ocrSelectionBtn.addEventListener('click', async () => {
    if (!canvas || !canvas.dataset.sel) {
      result.textContent = 'No selection found. Draw a box first.';
      return;
    }
    if (!backendRawPath) {
      result.textContent = 'You must upload the image (submit) first so server has a copy.';
      return;
    }
    const sel = JSON.parse(canvas.dataset.sel);
    // map back to natural image coords
    const natX = Math.round(sel.x / scale);
    const natY = Math.round(sel.y / scale);
    const natW = Math.round(sel.w / scale);
    const natH = Math.round(sel.h / scale);
    const lang = document.getElementById('lang').value || '';
    result.textContent = 'Running OCR on selection...';
    try {
      const res = await fetch('/CP/A0/CPA0_0100/ocr-bbox', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ path: backendRawPath, x: natX, y: natY, w: natW, h: natH, lang })
      });
      const data = await res.json();
      if (data.ok) {
        result.textContent = data.text || '(no text)';
      } else {
        result.textContent = 'Error: ' + (data.error || 'unknown');
      }
    } catch (err) {
      result.textContent = 'Request failed: ' + err;
    }
  });
});
