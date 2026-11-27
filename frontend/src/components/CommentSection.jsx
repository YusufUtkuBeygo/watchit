import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import usePagedList from '../hooks/usePagedList';
import LoadMore from './LoadMore';
import {
  getCommentsByVideoId, createComment, deleteComment, updateComment, likeComment, unlikeComment, pinComment, unpinComment,
} from '../services/commentService';
import { TOO_MANY_REQUESTS } from '../utils/errors';
import './CommentSection.css';
import Avatar from './Avatar';
import { CommentsSkeleton } from './Skeleton';
import { MessageSquare, ThumbsUp, Pin } from 'lucide-react';
import EmptyState from './EmptyState';
import ReportModal from './ReportModal';

//Yorum içeriği veritabanında en fazla 1000 karakter (comments.content varchar(1000))
const MAX_LENGTH = 1000;

//İzleme sayfasının altındaki yorum bölümü
//isVideoOwner: izleyen videonun sahibi mi (sahip yorumları sabitler ve videosundaki yorumları silebilir)
export default function CommentSection({ videoId, isVideoOwner = false }) {
  const { user } = useAuth();
  //'new' (en yeni) | 'top' (en beğenilen)
  const [sort, setSort] = useState('new');
  //Sayfa sayfa: her sayfada 24 üst yorum + cevapları. Yorum eklenince/silinince ya da sıralama değişince liste baştan yüklenir
  const list = usePagedList((page) => getCommentsByVideoId(videoId, page, sort), `comments-${videoId}-${sort}`);
  const { items: comments, status, reload } = list;
  //Hangi yoruma cevap yazılıyor (üst yorumun id'si), yoksa null
  const [replyingTo, setReplyingTo] = useState(null);

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

  //Sabitle / sabiti kaldır (yalnız video sahibi); sonra liste yeniden okunur (sabit en üste çıkar)
  const handlePin = async (comment) => {
    try {
      if (comment.pinned) await unpinComment(videoId);
      else await pinComment(comment.id);
      reload();
    } catch (error) {
      console.error('Yorum sabitlenemedi:', error);
      alert('Yorum sabitlenemedi.');
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
        Yorumlar
        {status === 'ready' && list.total > 0 && <span className="comments-count">{list.total}</span>}
      </h2>
      <label className="comments-sort">
        <span className="sr-only">Yorumları sırala</span>
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="new">En yeni</option>
          <option value="top">En beğenilen</option>
        </select>
      </label>

      {user ? (
        <CommentForm videoId={videoId} onPosted={reload} placeholder="Yorum ekle…" />
      ) : (
        <p className="comments-hint">Yorum yapmak için giriş yap.</p>
      )}

      {status === 'loading' && <CommentsSkeleton />}
      {status === 'error' && <p className="comments-hint">Yorumlar yüklenemedi.</p>}
      {status === 'ready' && comments.length === 0 && (
        <EmptyState
          compact
          icon={MessageSquare}
          title="Henüz yorum yok"
          text={user ? 'Video hakkında ne düşündüğünü ilk sen yaz.' : 'Giriş yapıp ilk yorumu sen yazabilirsin.'}
        />
      )}

      <ul className="comments-list">
        {topLevel.map((comment) => (
          <li key={comment.id}>
            <CommentItem
              comment={comment}
              canEdit={user?.id === comment.userId}
              canDelete={user?.id === comment.userId || isVideoOwner}
              canReport={!!user && user.id !== comment.userId}
              canLike={!!user}
              onPin={isVideoOwner ? () => handlePin(comment) : null}
              onDelete={handleDelete}
              onEdited={reload}
              onReply={user ? () => setReplyingTo(comment.id) : null}
            />

            {(repliesByParent[comment.id] || replyingTo === comment.id) && (
              <ul className="comments-replies">
                {(repliesByParent[comment.id] || []).map((reply) => (
                  <li key={reply.id}>
                    <CommentItem
                      comment={reply}
                      canEdit={user?.id === reply.userId}
                      canDelete={user?.id === reply.userId || isVideoOwner}
                      canReport={!!user && user.id !== reply.userId}
                      canLike={!!user}
                      onDelete={handleDelete}
                      onEdited={reload}
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
                      placeholder="Yanıt ekle…"
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
      <LoadMore list={list} label="Daha fazla yorum" />
    </section>
  );
}

//Tek bir yorum: kullanıcı adı, tarih, metin, Yanıtla / Düzenle / Sil
//canDelete: yorum benimse true (yalnız yazarı düzenleyip silebilir)
function CommentItem({ comment, canEdit, canDelete, canReport, canLike, onPin, onDelete, onReply, onEdited }) {
  const [isEditing, setIsEditing] = useState(false);
  const [isReporting, setIsReporting] = useState(false);

  return (
    <div className="comment">
      <Avatar className="comment-avatar" name={comment.username} />
      <div className="comment-body">
        {comment.pinned && (
          <div className="comment-pinned"><Pin size={14} aria-hidden="true" /> Video sahibi sabitledi</div>
        )}
        <div className="comment-meta">
          <span className="comment-author">{comment.username || 'Silinmiş kullanıcı'}</span>
          <span className="comment-date">
            {new Date(comment.createdAt).toLocaleDateString('tr-TR')}
            {comment.edited && ' (düzenlendi)'}
          </span>
        </div>
        {isEditing ? (
          <CommentEditForm
            comment={comment}
            onCancel={() => setIsEditing(false)}
            onSaved={() => { setIsEditing(false); onEdited(); }}
          />
        ) : (
          <>
            <p className="comment-content">{comment.content}</p>
            <div className="comment-actions">
              <CommentLike key={`${comment.id}-${comment.likedByMe}`} comment={comment} canLike={canLike} />
              {onReply && <button className="comment-action" onClick={onReply}>Yanıtla</button>}
              {canEdit && <button className="comment-action" onClick={() => setIsEditing(true)}>Düzenle</button>}
              {canDelete && <button className="comment-action" onClick={() => onDelete(comment.id)}>Sil</button>}
              {onPin && (
                <button className="comment-action" onClick={onPin}>{comment.pinned ? 'Sabitlemeyi kaldır' : 'Sabitle'}</button>
              )}
              {canReport && (
                <button className="comment-action" onClick={() => setIsReporting(true)}>Şikayet et</button>
              )}
            </div>
          </>
        )}
        {isReporting && (
          <ReportModal targetType="COMMENT" targetId={comment.id} onClose={() => setIsReporting(false)} />
        )}
      </div>
    </div>
  );
}

//Yorum beğenisi: anında değişir (iyimser), sunucu reddederse geri alınır. Girişsizde yalnız sayı görünür
function CommentLike({ comment, canLike }) {
  const [liked, setLiked] = useState(comment.likedByMe);
  const [count, setCount] = useState(comment.likeCount);
  const [busy, setBusy] = useState(false);

  const toggle = async () => {
    if (busy) return;
    const next = !liked;
    setBusy(true);
    setLiked(next);
    setCount((c) => c + (next ? 1 : -1));
    try {
      if (next) await likeComment(comment.id);
      else await unlikeComment(comment.id);
    } catch (error) {
      const status = error.response ? error.response.status : null;
      //409: zaten beğenilmiş, 404: zaten kaldırılmış (başka sekmede); ekrandaki doğru, geri alma
      if (status !== 409 && status !== 404) {
        setLiked(!next);
        setCount((c) => c + (next ? -1 : 1));
        if (status === 429) alert(TOO_MANY_REQUESTS);
      }
    } finally {
      setBusy(false);
    }
  };

  if (!canLike) {
    return count > 0 ? (
      <span className="comment-like-count"><ThumbsUp size={14} aria-hidden="true" /> {count}</span>
    ) : null;
  }
  return (
    <button type="button" className={`comment-action comment-like ${liked ? 'is-liked' : ''}`} onClick={toggle}
      aria-pressed={liked} aria-label={liked ? `Beğeniyi kaldır (${count})` : `Beğen (${count})`}>
      <ThumbsUp size={14} aria-hidden="true" /> {count > 0 && count}
    </button>
  );
}

//Yorum düzenleme kutusu: yalnız metin değişir
function CommentEditForm({ comment, onCancel, onSaved }) {
  const [content, setContent] = useState(comment.content);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);
    try {
      await updateComment(comment.id, content.trim(), comment.videoId);
      onSaved();
    } catch (error) {
      const status = error.response ? error.response.status : null;
      setErrorMessage(status === 401 ? 'Oturumun süresi dolmuş, tekrar giriş yap.' : 'Yorum kaydedilemedi.');
      setIsSubmitting(false);
    }
  };

  return (
    <form className="comment-form" onSubmit={handleSubmit}>
      <textarea
        className="comment-input"
        name="content"
        aria-label="Yorumu düzenle"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        maxLength={MAX_LENGTH}
        rows={2}
        autoFocus
        disabled={isSubmitting}
      />
      {errorMessage && <p className="comment-error">{errorMessage}</p>}
      <div className="comment-form-actions">
        <button type="button" className="comment-cancel" onClick={onCancel}>İptal</button>
        <button type="submit" className="comment-submit"
          disabled={isSubmitting || !content.trim() || content.trim() === comment.content}>
          {isSubmitting ? 'Kaydediliyor…' : 'Kaydet'}
        </button>
      </div>
    </form>
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
        status === 401 ? 'Oturumun süresi dolmuş, tekrar giriş yap.'
          : status === 429 ? TOO_MANY_REQUESTS : 'Yorum gönderilemedi.'
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
          {isSubmitting ? 'Gönderiliyor…' : parentCommentId ? 'Yanıtla' : 'Yorum yap'}
        </button>
      </div>
    </form>
  );
}
