import { useState, useEffect } from 'react';
import { getDocuments, uploadDocument, getDocumentStatus } from '../services/api';
import { removeToken } from '../services/auth';
import { useNavigate } from 'react-router-dom';
import ChatWindow from '../components/ChatWindow';

export default function DashboardPage() {
  const [documents, setDocuments] = useState([]);
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [uploading, setUploading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => { loadDocuments(); }, []);

  const loadDocuments = async () => {
    try {
      const res = await getDocuments();
      setDocuments(res.data);
    } catch (err) { console.error(err); }
  };

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    try {
      const res = await uploadDocument(file);
      const newDoc = res.data;
      setDocuments(prev => [newDoc, ...prev]);
      pollStatus(newDoc.id);
    } catch (err) {
      alert('Upload failed: ' + (err.response?.data?.error || err.message));
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  const pollStatus = (docId) => {
    const interval = setInterval(async () => {
      try {
        const res = await getDocumentStatus(docId);
        const { status, totalChunks } = res.data;
        setDocuments(prev => prev.map(d =>
          d.id === docId ? { ...d, status, totalChunks } : d
        ));
        if (status === 'READY' || status === 'FAILED') clearInterval(interval);
      } catch { clearInterval(interval); }
    }, 3000);
  };

  return (
    <div style={{ display:'flex', height:'100vh',
      fontFamily:'-apple-system, BlinkMacSystemFont, sans-serif' }}>

      <div style={{ width:260, background:'#1a1a2e', color:'white',
        display:'flex', flexDirection:'column' }}>
        <div style={{ padding:'20px 16px', borderBottom:'1px solid #2a2a4a' }}>
          <div style={{ fontSize:20, fontWeight:700, color:'#4da6ff',
            marginBottom:12 }}>DocMind</div>
          <label style={{ display:'flex', alignItems:'center', gap:8,
            padding:'10px 12px', background:'#2a2a4a', borderRadius:8,
            cursor:'pointer', fontSize:13, color:'#ccc' }}>
            <span>+</span>
            <span>{uploading ? 'Uploading...' : 'Upload document'}</span>
            <input type="file" accept=".pdf,.docx,.txt"
              onChange={handleUpload} style={{ display:'none' }}
              disabled={uploading} />
          </label>
        </div>

        <div style={{ flex:1, overflowY:'auto', padding:8 }}>
          <div style={{ fontSize:10, fontWeight:600, color:'#555',
            padding:'8px 8px 4px', letterSpacing:'0.05em' }}>
            YOUR DOCUMENTS
          </div>
          {documents.map(doc => (
            <div key={doc.id} onClick={() => setSelectedDoc(doc)}
              style={{ padding:'10px 12px', borderRadius:8, cursor:'pointer',
                marginBottom:2, display:'flex', alignItems:'center', gap:10,
                background: selectedDoc?.id===doc.id ? '#2a2a4a' : 'transparent' }}>
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ fontSize:12, fontWeight:500, color:'#ddd',
                  overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                  {doc.filename}
                </div>
                <div style={{ fontSize:10, color:'#666', marginTop:2 }}>
                  {doc.status==='READY' ? `${doc.totalChunks} chunks indexed` : doc.status}
                </div>
              </div>
              <div style={{ width:8, height:8, borderRadius:'50%', flexShrink:0,
                background: doc.status==='READY' ? '#22c55e' :
                            doc.status==='FAILED' ? '#ef4444' : '#f59e0b' }} />
            </div>
          ))}
          {documents.length === 0 && (
            <div style={{ fontSize:12, color:'#555', padding:'16px 8px',
              textAlign:'center' }}>
              No documents yet. Upload a PDF to start.
            </div>
          )}
        </div>

        <div style={{ padding:'12px 16px', borderTop:'1px solid #2a2a4a' }}>
          <button onClick={() => { removeToken(); navigate('/login'); }}
            style={{ background:'none', border:'none', color:'#666',
              cursor:'pointer', fontSize:13 }}>
            Sign out
          </button>
        </div>
      </div>

      <div style={{ flex:1, display:'flex', flexDirection:'column',
        background:'#f8f9fa' }}>
        {selectedDoc ? (
          <ChatWindow document={selectedDoc} />
        ) : (
          <div style={{ flex:1, display:'flex', alignItems:'center',
            justifyContent:'center' }}>
            <div style={{ textAlign:'center', color:'#999' }}>
              <div style={{ fontSize:48, marginBottom:16 }}>📄</div>
              <div style={{ fontSize:18, fontWeight:500, color:'#555',
                marginBottom:8 }}>Select a document</div>
              <div style={{ fontSize:14 }}>
                Upload a PDF or select one from the sidebar to start
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}