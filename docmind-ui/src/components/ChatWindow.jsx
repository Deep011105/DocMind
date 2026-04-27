import { useState, useEffect, useRef } from 'react';
import { sendQuery } from '../services/api';
import SourceChip from './SourceChip';

export default function ChatWindow({ document }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    setMessages([{
      role:'bot',
      text:`Document loaded: ${document.filename}. Ask me anything and I'll answer using only what's in the document.`,
      sources:[]
    }]);
  }, [document]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior:'smooth' });
  }, [messages]);

  const handleSend = async () => {
    const q = input.trim();
    if (!q || loading) return;
    setMessages(prev => [...prev, { role:'user', text:q }]);
    setInput('');
    setLoading(true);
    try {
      const res = await sendQuery(q, document.id);
      setMessages(prev => [...prev, {
        role:'bot', text:res.data.answer,
        sources:res.data.sources||[], responseTime:res.data.responseTimeMs
      }]);
    } catch {
      setMessages(prev => [...prev, {
        role:'bot', text:'Something went wrong. Please try again.', sources:[]
      }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display:'flex', flexDirection:'column', height:'100%' }}>
      <div style={{ padding:'12px 20px', borderBottom:'1px solid #e5e5e5',
        background:'white', display:'flex', justifyContent:'space-between',
        alignItems:'center' }}>
        <div style={{ fontSize:14, fontWeight:500, color:'#333' }}>
          {document.filename}
        </div>
        <div style={{ display:'flex', gap:8 }}>
          <span style={{ fontSize:11, padding:'3px 8px',
            background:'#e8f5e9', color:'#2e7d32', borderRadius:20 }}>
            Mistral 7B · local
          </span>
          <span style={{ fontSize:11, padding:'3px 8px',
            background:'#e3f2fd', color:'#1565c0', borderRadius:20 }}>
            {document.totalChunks} chunks
          </span>
        </div>
      </div>

      <div style={{ flex:1, overflowY:'auto', padding:20,
        display:'flex', flexDirection:'column', gap:16 }}>
        {messages.map((msg, i) => (
          <div key={i} style={{ display:'flex', flexDirection:'column',
            alignItems: msg.role==='user' ? 'flex-end' : 'flex-start' }}>
            <div style={{ maxWidth:'78%', padding:'10px 14px',
              fontSize:14, lineHeight:1.6,
              background: msg.role==='user' ? '#0066cc' : 'white',
              color: msg.role==='user' ? 'white' : '#333',
              border: msg.role==='bot' ? '1px solid #e5e5e5' : 'none',
              borderRadius: msg.role==='user' ?
                '12px 12px 2px 12px' : '12px 12px 12px 2px' }}>
              {msg.text}
            </div>
            {msg.sources?.length > 0 && (
              <div style={{ display:'flex', gap:6, flexWrap:'wrap',
                marginTop:6, maxWidth:'78%' }}>
                {msg.sources.map((src, j) => <SourceChip key={j} source={src} />)}
              </div>
            )}
            {msg.responseTime && (
              <div style={{ fontSize:10, color:'#999', marginTop:4 }}>
                {(msg.responseTime/1000).toFixed(1)}s · Ollama local
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div style={{ display:'flex', gap:4, padding:'12px 14px',
            background:'white', border:'1px solid #e5e5e5',
            borderRadius:'12px 12px 12px 2px', width:'fit-content' }}>
            {[0,1,2].map(i => (
              <div key={i} style={{ width:6, height:6, borderRadius:'50%',
                background:'#ddd' }} />
            ))}
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div style={{ padding:'6px 20px', background:'#f0fdf4',
        borderTop:'1px solid #e5e5e5', fontSize:11, color:'#16a34a' }}>
        🔒 Running locally on Ollama — no data leaves your machine
      </div>

      <div style={{ padding:'14px 20px', background:'white',
        borderTop:'1px solid #e5e5e5' }}>
        <div style={{ display:'flex', gap:10, alignItems:'flex-end' }}>
          <textarea
            style={{ flex:1, padding:'10px 14px', border:'1px solid #ddd',
              borderRadius:12, fontSize:14, resize:'none', outline:'none',
              minHeight:42, maxHeight:120, fontFamily:'inherit' }}
            placeholder={`Ask anything about ${document.filename}...`}
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => {
              if (e.key==='Enter' && !e.shiftKey) {
                e.preventDefault(); handleSend();
              }
            }}
            rows={1}
          />
          <button onClick={handleSend}
            disabled={!input.trim() || loading}
            style={{ width:42, height:42, borderRadius:'50%',
              background: input.trim() ? '#0066cc' : '#e5e5e5',
              border:'none', cursor: input.trim() ? 'pointer' : 'default',
              color:'white', fontSize:18, flexShrink:0 }}>
            ↑
          </button>
        </div>
      </div>
    </div>
  );
}