import { Link } from 'react-router-dom';

export default function HomePage() {
  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#0f172a',
      backgroundImage: 'radial-gradient(circle at 50% 0%, #1e1b4b 0%, #0f172a 70%)',
      color: 'white',
      fontFamily: '"Inter", -apple-system, sans-serif',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '0 20px',
      overflow: 'hidden',
      position: 'relative'
    }}>
      {/* Background Orbs */}
      <div style={{
        position: 'absolute',
        top: '-10%',
        left: '-5%',
        width: '50vw',
        height: '50vw',
        background: 'radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 60%)',
        borderRadius: '50%',
        zIndex: 0
      }} />
      <div style={{
        position: 'absolute',
        bottom: '-10%',
        right: '-5%',
        width: '60vw',
        height: '60vw',
        background: 'radial-gradient(circle, rgba(168,85,247,0.15) 0%, transparent 60%)',
        borderRadius: '50%',
        zIndex: 0
      }} />

      <style>
        {`
          @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');
          
          @keyframes fadeUp {
            from { opacity: 0; transform: translateY(30px); }
            to { opacity: 1; transform: translateY(0); }
          }
          
          .btn-primary {
            background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
            color: white;
            padding: 16px 36px;
            border-radius: 9999px;
            font-size: 16px;
            font-weight: 600;
            text-decoration: none;
            transition: all 0.3s ease;
            box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.5);
            display: inline-block;
          }
          
          .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 15px 30px -5px rgba(99, 102, 241, 0.6);
          }
          
          .btn-secondary {
            background: rgba(255, 255, 255, 0.1);
            color: white;
            padding: 16px 36px;
            border-radius: 9999px;
            font-size: 16px;
            font-weight: 600;
            text-decoration: none;
            transition: all 0.3s ease;
            backdrop-filter: blur(10px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            display: inline-block;
          }
          
          .btn-secondary:hover {
            background: rgba(255, 255, 255, 0.15);
            transform: translateY(-2px);
          }
          
          .glass-card {
            background: rgba(30, 41, 59, 0.7);
            backdrop-filter: blur(20px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 24px;
            padding: 40px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            text-align: center;
            max-width: 800px;
            width: 100%;
            z-index: 10;
            animation: fadeUp 1s ease-out forwards;
          }
          
          .title-gradient {
            background: linear-gradient(to right, #818cf8, #c084fc);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            display: inline-block;
          }
        `}
      </style>

      <div className="glass-card">
        <div style={{ 
          fontSize: 14, 
          fontWeight: 600, 
          letterSpacing: 2, 
          color: '#818cf8',
          textTransform: 'uppercase',
          marginBottom: 16
        }}>
          Introducing
        </div>
        
        <h1 style={{ 
          fontSize: 'clamp(40px, 8vw, 72px)', 
          fontWeight: 800, 
          lineHeight: 1.1, 
          margin: '0 0 24px',
          letterSpacing: '-0.02em'
        }}>
          Talk to your documents with <span className="title-gradient">DocMind</span>
        </h1>
        
        <p style={{ 
          fontSize: 'clamp(16px, 3vw, 20px)', 
          lineHeight: 1.6, 
          color: '#94a3b8',
          margin: '0 auto 40px',
          maxWidth: '600px'
        }}>
          Securely upload your private PDFs and documents. Ask questions, extract insights, and get instant answers powered by AI.
        </p>
        
        <div style={{ display: 'flex', gap: 16, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/register" className="btn-primary">
            Get Started Free
          </Link>
          <Link to="/login" className="btn-secondary">
            Sign In
          </Link>
        </div>
      </div>
      
      {/* Decorative Mockup */}
      <div style={{
        marginTop: 60,
        width: '100%',
        maxWidth: 900,
        height: 300,
        background: 'linear-gradient(180deg, rgba(30,41,59,0.8) 0%, rgba(15,23,42,0) 100%)',
        borderTop: '1px solid rgba(255,255,255,0.1)',
        borderLeft: '1px solid rgba(255,255,255,0.05)',
        borderRight: '1px solid rgba(255,255,255,0.05)',
        borderTopLeftRadius: 16,
        borderTopRightRadius: 16,
        zIndex: 10,
        position: 'relative',
        animation: 'fadeUp 1.2s ease-out forwards',
        opacity: 0,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}>
        <div style={{ height: 40, borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', padding: '0 16px', gap: 8 }}>
          <div style={{ width: 12, height: 12, borderRadius: '50%', background: '#ef4444' }}></div>
          <div style={{ width: 12, height: 12, borderRadius: '50%', background: '#f59e0b' }}></div>
          <div style={{ width: 12, height: 12, borderRadius: '50%', background: '#10b981' }}></div>
        </div>
        <div style={{ flex: 1, padding: 24, display: 'flex', gap: 16 }}>
          <div style={{ flex: 1, background: 'rgba(255,255,255,0.02)', borderRadius: 8 }}></div>
          <div style={{ flex: 2, background: 'rgba(255,255,255,0.02)', borderRadius: 8, display: 'flex', flexDirection: 'column', padding: 16, gap: 12 }}>
            <div style={{ width: '80%', height: 12, background: 'rgba(255,255,255,0.05)', borderRadius: 6 }}></div>
            <div style={{ width: '60%', height: 12, background: 'rgba(255,255,255,0.05)', borderRadius: 6 }}></div>
            <div style={{ width: '90%', height: 12, background: 'rgba(255,255,255,0.05)', borderRadius: 6 }}></div>
          </div>
        </div>
      </div>
    </div>
  );
}
