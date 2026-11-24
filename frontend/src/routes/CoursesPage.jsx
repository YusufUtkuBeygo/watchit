import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { GraduationCap } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getCourses } from '../services/courseService';
import Thumbnail from '../components/Thumbnail';
import EmptyState from '../components/EmptyState';
import { PlaylistGridSkeleton } from '../components/Skeleton';
import './PlaylistsPage.css';
import './ListPage.css';

//Kurslar (/courses): kanalların "kurs" olarak yayınladığı sıralı listeler.
//Girişliyken başladığın kurslar ilerlemeyle üstte ayrı durur (YouTube'daki "Kurslarım" gibi)
function CoursesPage() {
  const { user } = useAuth();
  const [courses, setCourses] = useState([]);
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');

  //Giriş/çıkışta yeniden okunur: ilerleme kişiye özel
  useEffect(() => {
    let ignore = false;
    getCourses()
      .then((list) => { if (!ignore) { setCourses(list); setStatus('ready'); } })
      .catch((error) => {
        console.error('Kurslar yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, [user]);

  const started = courses.filter((course) => course.watchedCount > 0);
  const others = courses.filter((course) => course.watchedCount === 0);

  return (
    <div className="page">
      <h1 className="page-title">Kurslar</h1>
      {status === 'loading' && <PlaylistGridSkeleton />}
      {status === 'error' && <p className="grid-message">Kurslar yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && courses.length === 0 && (
        <EmptyState
          icon={GraduationCap}
          title="Henüz kurs yok"
          text="Kanal sahipleri bir oynatma listesini düzenlerken “Kurs olarak yayınla”yı seçince burada görünür."
          action={<Link to="/playlists" className="btn">Oynatma listelerin</Link>}
        />
      )}

      {started.length > 0 && (
        <section className="course-section" aria-labelledby="kurslarim">
          <h2 id="kurslarim" className="section-heading">Devam ettiğin kurslar</h2>
          <CourseGrid courses={started} />
        </section>
      )}
      {others.length > 0 && (
        <section className="course-section" aria-labelledby="tum-kurslar">
          {started.length > 0 && <h2 id="tum-kurslar" className="section-heading">Diğer kurslar</h2>}
          <CourseGrid courses={others} />
        </section>
      )}
    </div>
  );
}

function CourseGrid({ courses }) {
  return (
    <ul className="playlist-grid">
      {courses.map((course) => {
        const done = Math.min(course.watchedCount, course.videoCount);
        const percent = course.videoCount > 0 ? done / course.videoCount : 0;
        return (
          <li key={course.id} className="course-card">
            <Link to={`/playlist/${course.id}`} className="playlist-card">
              <div className="playlist-cover">
                <Thumbnail
                  src={course.thumbnailUrl}
                  title={course.title}
                  icon={<GraduationCap size={32} aria-hidden="true" />}
                  badge={`${course.videoCount} ders`}
                >
                  {done > 0 && (
                    <span className="row-progress"><span style={{ width: `${percent * 100}%` }} /></span>
                  )}
                </Thumbnail>
              </div>
              <span className="playlist-title">{course.title}</span>
              <span className="playlist-visibility">{course.ownerName}</span>
              {done > 0 && (
                <span className="playlist-visibility">{done}/{course.videoCount} ders izlendi</span>
              )}
            </Link>
            {course.firstVideoId && done === 0 && (
              <Link to={`/watch/${course.firstVideoId}`} className="btn btn-primary course-start">Kursa başla</Link>
            )}
          </li>
        );
      })}
    </ul>
  );
}

export default CoursesPage;
