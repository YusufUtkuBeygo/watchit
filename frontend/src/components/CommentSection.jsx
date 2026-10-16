import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { getCommentsByVideoId, createComment, deleteComment } from '../services/commentService';
import './CommentSection.css';

//Yorum içeriği veritabanında en fazla 1000 karakter (comments.content varchar(1000))
const MAX_LENGTH = 1000;

//İzleme sayfasının altındaki yorum bölümü
export default function CommentSection({ videoId }) {
  const { user } = useAuth();
  const [comments, setComments] = useState([]);
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');
  //Yorum eklenince/silinince artar, liste yeniden çekilir
  const [reloadKey, setReloadKey] = useState(0);
  //Hangi yoruma cevap yazılıyor (üst yorumun id'si), yoksa null
  const [replyingTo, setReplyingTo] = useState(null);

  useEffect(() => {
    let ignore = false;

    async function loadComments() {
      try {
        const data = await getCommentsByVideoId(videoId);
        if (ignore) return;
        setComments(data);
        setStatus('ready');
      } catch (error) {
        console.error('Yorumlar yüklenemedi:', error);
        if (!ignore) setStatus('error');
      }
    }

    loadComments();
    return () => { ignore = true; };
  }, [videoId, reloadKey]);

  const reload = () => setReloadKey((key) => key + 1);

  const handleDelete = async (commentId) => {
    if (!window.confirm('Yorum silinsin mi?')) return;
    try {
      await deleteComment(commentId);
      reload();
    } catch (error) {
      console.error('Yorum silinemedi:', error);
      alert('Yorum silinemedi.');
    }
  };

  //Liste düz geliyor: üst yorumları ayır, cevapları üst yorumun id'sine göre grupla
  const topLevel = [];
  const repliesByParent = {};
  const ids = new Set(comments.map((c) => c.id));
  comments.forEach((comment) => {
    //Üst yorumu silinmiş (ya da listede olmayan) cevap üst yorum gibi gösterilir
    if (comment.parentCommentId && ids.has(comment.parentCommentId)) {
      (repliesByParent[comment.parentCommentId] ??= []).push(comment);
    } else {
      topLevel.push(comment);
    }
  });
  //Cevaplar eskiden yeniye okunur (YouTube gibi); liste yeniden eskiye geldiği için ters çevir
  Object.values(repliesByParent).forEach((replies) => replies.reverse());

  return (
    <section className="comments">
      <h2 className="comments-title">
        {status === 'ready' ? `${comments.length} yorum` : 'Yorumlar'}
      </h2>

      {user ? (
        <CommentForm videoId={videoId} onPosted={reload} placeholder="Yorum ekle..." />
      ) : (
        <p className="comments-hint">Yorum yapmak için giriş yap.</p>
      )}

      {status === 'loading' && <p className="comments-hint">Yorumlar yükleniyor...</p>}
      {status === 'error' && <p className="comments-hint">Yorumlar yüklenemedi.</p>}
      {status === 'ready' && comments.length === 0 && (
        <p className="comments-hint">Henüz yorum yok. İlk yorumu sen yaz.</p>
      )}

      <ul className="comments-list">
        {topLevel.map((comment) => (
          <li key={comment.id}>
            <CommentItem
              comment={comment}
              canDelete={user?.id === comment.userId}
              onDelete={handleDelete}
              onReply={user ? () => setReplyingTo(comment.id) : null}
            />

            {(repliesByParent[comment.id] || replyingTo === comment.id) && (
              <ul className="comments-replies">
                {(repliesByParent[comment.id] || []).map((reply) => (
                  <li key={reply.id}>
                    <CommentItem
                      comment={reply}
                      canDelete={user?.id === reply.userId}
                      onDelete={handleDelete}
                      //Cevaba cevap da aynı üst yoruma bağlanır (tek seviye, YouTube gibi)
                      onReply={user ? () => setReplyingTo(comment.id) : null}
                    />
                  </li>
                ))}
                {replyingTo === comment.id && (
                  <li>
                    <CommentForm
                      videoId={videoId}
                      parentCommentId={comment.id}
                      placeholder="Yanıt ekle..."
                      autoFocus
                      onPosted={() => { setReplyingTo(null); reload(); }}
                      onCancel={() => setReplyingTo(null)}
                    />
                  </li>
                )}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

//Tek bir yorum: kullanıcı adı, tarih, metin, Yanıtla / Sil
function CommentItem({ comment, canDelete, onDelete, onReply }) {
  return (
    <div className="comment">
      <div className="comment-avatar">{(comment.username || '?').charAt(0).toUpperCase()}</div>
      <div className="comment-body">
        <div className="comment-meta">
          <span className="comment-author">{comment.username || 'Silinmiş kullanıcı'}</span>
          <span className="comment-date">
            {new Date(comment.createdAt).toLocaleDateString('tr-TR')}
            {comment.edited && ' (düzenlendi)'}
          </span>
        </div>
        <p className="comment-content">{comment.content}</p>
        <div className="comment-actions">
          {onReply && <button className="comment-action" onClick={onReply}>Yanıtla</button>}
          {canDelete && (
            <button className="comment-action" onClick={() => onDelete(comment.id)}>Sil</button>
          )}
        </div>
      </div>
    </div>
  );
}

//Yorum ya da cevap yazma kutusu
function CommentForm({ videoId, parentCommentId = null, placeholder, autoFocus, onPosted, onCancel }) {
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);
    try {
      await createComment({ content: content.trim(), videoId, parentCommentId });
      setContent('');
      onPosted();
    } catch (error) {
      const status = error.response ? error.response.status : null;
      setErrorMessage(
        status === 401 ? 'Oturumun süresi dolmuş, tekrar giriş yap.' : 'Yorum gönderilemedi.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="comment-form" onSubmit={handleSubmit}>
      <textarea
        className="comment-input"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={placeholder}
        maxLength={MAX_LENGTH}
        rows={2}
        autoFocus={autoFocus}
        disabled={isSubmitting}
      />
      {errorMessage && <p className="comment-error">{errorMessage}</p>}
      <div className="comment-form-actions">
        {onCancel && (
          <button type="button" className="comment-cancel" onClick={onCancel}>İptal</button>
        )}
        <button type="submit" className="comment-submit" disabled={isSubmitting || !content.trim()}>
          {isSubmitting ? 'Gönderiliyor...' : parentCommentId ? 'Yanıtla' : 'Yorum yap'}
        </button>
      </div>
    </form>
  );
}
