import { useEffect, useRef, useState } from "react";
import {
  BookOpen,
  Plus,
  Search,
  ArrowUpRight,
  Sparkles,
  LayoutGrid,
  List,
  LogOut,
  Pencil,
  Trash2,
  Send,
  X,
} from "lucide-react";
import { useBlog } from "./components/useBlog";
import { useAuth } from "./context/AuthContext";
import type { Post } from "./types/types";
type Answer = {
  answer: string;
  mode: string;
  sources: { postId: number; title: string; excerpt: string }[];
};
export default function BlogApp() {
  const blog = useBlog();
  const { user, accessToken, logout } = useAuth();
  const [view, setView] = useState("library");
  const [query, setQuery] = useState("");
  const [layout, setLayout] = useState("grid");
  const [editor, setEditor] = useState<Post | "new" | null>(null);
  const [reading, setReading] = useState<Post | null>(null);
  const [removing, setRemoving] = useState<Post | null>(null);
  const [title, setTitle] = useState("");
  const [contents, setContents] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState<Answer | null>(null);
  const dialog = useRef<HTMLDialogElement>(null);
  const modalOpen = editor !== null || reading !== null || removing !== null;
  useEffect(() => {
    if (modalOpen) dialog.current?.showModal();
    else dialog.current?.close();
  }, [modalOpen]);
  function compose(post: Post | "new") {
    setEditor(post);
    setTitle(post === "new" ? "" : post.title);
    setContents(post === "new" ? "" : post.contents);
    setNotice("");
  }
  function close() {
    if (!busy) {
      setEditor(null);
      setReading(null);
      setRemoving(null);
      setNotice("");
    }
  }
  async function save(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      if (editor === "new")
        await blog.addPost({ title: title.trim(), contents: contents.trim() });
      else if (editor)
        await blog.editPost({
          id: editor.id,
          title: title.trim(),
          content: contents.trim(),
        });
      setEditor(null);
      setAnswer(null);
    } catch {
      setNotice(
        "Could not save your post. Your writing is still here; please try again.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function remove() {
    if (!removing) return;
    setBusy(true);
    try {
      await blog.deletePost(removing.id);
      setRemoving(null);
      setAnswer(null);
    } catch {
      setNotice("Could not delete this post. Please try again.");
    } finally {
      setBusy(false);
    }
  }
  async function ask(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setNotice("");
    setAnswer(null);
    try {
      const response = await fetch(
        `${import.meta.env.VITE_API_URL || "/api"}/assistant`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({ question }),
        },
      );
      if (!response.ok) throw new Error();
      setAnswer(await response.json());
    } catch {
      setNotice(
        "The assistant is unavailable. Check your connection and AI service, then try again.",
      );
    } finally {
      setBusy(false);
    }
  }
  const filtered = blog.posts.filter((p) =>
    `${p.title} ${p.contents}`.toLowerCase().includes(query.toLowerCase()),
  );
  const words = blog.posts.reduce(
    (n, p) => n + p.contents.split(/\s+/).filter(Boolean).length,
    0,
  );
  return (
    <div className="workspace">
      <aside className="sidebar">
        <a className="brand" href="/">
          <span className="brand-icon">
            <BookOpen size={21} />
          </span>
          folio<span className="brand-dot">.</span>
        </a>
        <span className="workspace-label">YOUR WORKSPACE</span>
        <nav aria-label="Workspace">
          <button
            className={view === "library" ? "nav active" : "nav"}
            onClick={() => {
              setView("library");
              setNotice("");
            }}
          >
            <BookOpen size={18} />
            All posts<span className="count">{blog.totalPosts}</span>
          </button>
          <button
            className={view === "assistant" ? "nav active" : "nav"}
            onClick={() => {
              setView("assistant");
              setNotice("");
            }}
          >
            <Sparkles size={18} />
            Ask Folio<span className="ai-label">AI</span>
          </button>
        </nav>
        <div className="sidebar-note">
          <span className="little-star">✳</span>
          <h3>
            A little space.
            <br />A lot of possibility.
          </h3>
          <p>Collect your ideas. Find the connections. Keep writing.</p>
          <button onClick={() => compose("new")}>
            Write something <ArrowUpRight size={16} />
          </button>
        </div>
        <div className="profile">
          <span className="avatar">
            {user?.username.slice(0, 1).toUpperCase()}
          </span>
          <div>
            <strong>{user?.username}</strong>
            <small>Personal workspace</small>
          </div>
          <button aria-label="Sign out" onClick={logout}>
            <LogOut size={17} />
          </button>
        </div>
      </aside>
      <main className="main">
        <header className="topbar">
          <span>
            Workspace <span className="slash">/</span>{" "}
            {view === "library" ? "All posts" : "Ask Folio"}
          </span>
          <span className="private-label">
            <span />
            Private library
          </span>
        </header>
        <div className="page">
          <div className="page-heading">
            <div>
              <p className="eyebrow">A HOME FOR YOUR IDEAS</p>
              <h1>
                {view === "library"
                  ? "Your words, together."
                  : "Find the thread."}
              </h1>
              <p className="subtitle">
                {view === "library"
                  ? "A thoughtful place to write, revisit, and connect the dots."
                  : "Ask questions. Discover connections across your own writing."}
              </p>
            </div>
            <button className="primary" onClick={() => compose("new")}>
              <Plus size={18} />
              New post
            </button>
          </div>
          {view === "library" ? (
            <>
              <section className="overview" aria-label="Library overview">
                <div className="metric">
                  <span className="metric-icon">
                    <BookOpen size={19} />
                  </span>
                  <div>
                    <p>Your library</p>
                    <strong>
                      {blog.totalPosts.toString().padStart(2, "0")}
                    </strong>
                    <small>posts in your collection</small>
                  </div>
                </div>
                <div className="metric peach">
                  <span className="metric-icon">
                    <Pencil size={19} />
                  </span>
                  <div>
                    <p>Words on this page</p>
                    <strong>{words.toLocaleString()}</strong>
                    <small>one idea at a time</small>
                  </div>
                </div>
                <button
                  className="assistant-teaser"
                  onClick={() => setView("assistant")}
                >
                  <Sparkles size={24} />
                  <div>
                    <strong>Your writing has answers.</strong>
                    <p>Explore your posts with Ask Folio.</p>
                    <span>
                      Start a conversation <ArrowUpRight size={16} />
                    </span>
                  </div>
                </button>
              </section>
              <section>
                <div className="library-heading">
                  <h2>
                    All posts <span>{blog.totalPosts}</span>
                  </h2>
                  <div className="library-tools">
                    <label className="search">
                      <Search size={17} />
                      <input
                        aria-label="Search posts on this page"
                        placeholder="Search this page…"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                      />
                    </label>
                    <div className="view-switch">
                      <button
                        aria-label="Grid view"
                        aria-pressed={layout === "grid"}
                        onClick={() => setLayout("grid")}
                      >
                        <LayoutGrid size={18} />
                      </button>
                      <button
                        aria-label="List view"
                        aria-pressed={layout === "list"}
                        onClick={() => setLayout("list")}
                      >
                        <List size={18} />
                      </button>
                    </div>
                  </div>
                </div>
                {blog.error && (
                  <p className="error" role="alert">
                    {blog.error}
                  </p>
                )}
                {blog.isLoading ? (
                  <p className="empty" role="status">
                    Opening your library…
                  </p>
                ) : filtered.length ? (
                  <div className={`posts ${layout}`}>
                    {filtered.map((post, i) => (
                      <article className={`post tone-${i % 3}`} key={post.id}>
                        <div className="post-top">
                          <span className="post-number">
                            {String((blog.page - 1) * 10 + i + 1).padStart(
                              2,
                              "0",
                            )}{" "}
                            / NOTE
                          </span>
                          <BookOpen size={18} />
                        </div>
                        <button
                          className="post-read"
                          onClick={() => setReading(post)}
                        >
                          <h3>{post.title}</h3>
                          <p>{post.contents}</p>
                        </button>
                        <div className="post-footer">
                          <span>
                            {Math.max(
                              1,
                              Math.ceil(
                                post.contents.split(/\s+/).length / 200,
                              ),
                            )}{" "}
                            min read
                          </span>
                          <div>
                            <button
                              aria-label={`Edit ${post.title}`}
                              onClick={() => compose(post)}
                            >
                              <Pencil size={16} />
                            </button>
                            <button
                              aria-label={`Delete ${post.title}`}
                              onClick={() => {
                                setNotice("");
                                setRemoving(post);
                              }}
                            >
                              <Trash2 size={16} />
                            </button>
                            <button
                              aria-label={`Read ${post.title}`}
                              onClick={() => setReading(post)}
                            >
                              <ArrowUpRight size={19} />
                            </button>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="empty">
                    <BookOpen size={35} />
                    <h3>
                      {query
                        ? "No matching posts on this page"
                        : "Every idea starts somewhere."}
                    </h3>
                    <p>
                      {query
                        ? "Try another search or change pages."
                        : "Write your first post and make this space your own."}
                    </p>
                    {!query && (
                      <button
                        className="primary"
                        onClick={() => compose("new")}
                      >
                        <Plus size={17} />
                        Write your first post
                      </button>
                    )}
                  </div>
                )}
                <footer className="pagination">
                  <span>
                    Page {blog.page} of {blog.totalPages}
                  </span>
                  <div>
                    <button
                      disabled={blog.page <= 1 || blog.isLoading}
                      onClick={() => blog.changePage(blog.page - 1)}
                    >
                      Previous
                    </button>
                    <button
                      disabled={blog.page >= blog.totalPages || blog.isLoading}
                      onClick={() => blog.changePage(blog.page + 1)}
                    >
                      Next
                    </button>
                  </div>
                </footer>
              </section>
            </>
          ) : (
            <section className="assistant-panel">
              <div className="assistant-intro">
                <span className="assistant-mark">
                  <Sparkles size={30} />
                </span>
                <h2>A conversation with your library.</h2>
                <p>
                  Folio searches your posts and shows the passages behind each
                  answer. Generated answers can make mistakes; check the
                  sources.
                </p>
              </div>
              <div className="suggestions">
                {[
                  "What have I written about design?",
                  "What ideas appear across my posts?",
                  "What have I learned about testing?",
                ].map((q) => (
                  <button key={q} onClick={() => setQuestion(q)}>
                    {q}
                    <ArrowUpRight size={15} />
                  </button>
                ))}
              </div>
              <form className="ask-form" onSubmit={ask}>
                <label htmlFor="question">Your question</label>
                <div>
                  <input
                    id="question"
                    value={question}
                    maxLength={1000}
                    required
                    onChange={(e) => setQuestion(e.target.value)}
                    placeholder="Ask something about your posts…"
                  />
                  <button
                    className="primary"
                    disabled={busy || !question.trim()}
                  >
                    <Send size={18} />
                    {busy ? "Searching…" : "Ask Folio"}
                  </button>
                </div>
              </form>
              {notice && (
                <p className="error" role="alert">
                  {notice}
                </p>
              )}
              {answer && (
                <div className="answer" aria-live="polite">
                  <span className="mode">
                    {answer.mode === "rag"
                      ? "AI answer · grounded in your posts"
                      : "Library search"}
                  </span>
                  <p>{answer.answer}</p>
                  <h3>Sources</h3>
                  {answer.sources.length === 0 ? (
                    <p>No matching sources.</p>
                  ) : (
                    answer.sources.map((s, i) => (
                      <div className="source" key={`${s.postId}-${i}`}>
                        <strong>
                          [{s.postId}] {s.title}
                        </strong>
                        <p>{s.excerpt}</p>
                      </div>
                    ))
                  )}
                </div>
              )}
            </section>
          )}
          <div className="page-bottom">
            <span>folio.</span>
            <p>Make room for your next idea.</p>
          </div>
        </div>
      </main>
      <dialog
        ref={dialog}
        onCancel={(e) => {
          e.preventDefault();
          close();
        }}
        onClick={(e) => {
          if (e.target === dialog.current) close();
        }}
      >
        <div className="dialog-body">
          <button
            className="close"
            aria-label="Close dialog"
            disabled={busy}
            onClick={close}
          >
            <X size={22} />
          </button>
          {editor !== null ? (
            <form onSubmit={save}>
              <span className="mode">
                {editor === "new" ? "A fresh page" : "Make it your own"}
              </span>
              <h2>
                {editor === "new" ? "Let the idea unfold." : "Edit your post."}
              </h2>
              <label htmlFor="post-title">Title</label>
              <input
                autoFocus
                id="post-title"
                required
                maxLength={200}
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
              <label htmlFor="post-content">Your writing</label>
              <textarea
                id="post-content"
                required
                maxLength={50000}
                rows={12}
                value={contents}
                onChange={(e) => setContents(e.target.value)}
              />
              {notice && (
                <p role="alert" className="error">
                  {notice}
                </p>
              )}
              <div className="dialog-actions">
                <button type="button" disabled={busy} onClick={close}>
                  Cancel
                </button>
                <button
                  className="primary"
                  disabled={busy || !title.trim() || !contents.trim()}
                >
                  {busy ? "Saving…" : "Save post"}
                </button>
              </div>
            </form>
          ) : removing ? (
            <>
              <h2>Delete this post?</h2>
              <p>
                “{removing.title}” will be permanently removed from your library
                and future assistant results.
              </p>
              {notice && (
                <p role="alert" className="error">
                  {notice}
                </p>
              )}
              <div className="dialog-actions">
                <button disabled={busy} onClick={close}>
                  Keep post
                </button>
                <button className="danger" disabled={busy} onClick={remove}>
                  {busy ? "Deleting…" : "Delete post"}
                </button>
              </div>
            </>
          ) : reading ? (
            <article className="reader">
              <span className="mode">From your library</span>
              <h2>{reading.title}</h2>
              <p>{reading.contents}</p>
              <button onClick={close}>Back to library</button>
            </article>
          ) : null}
        </div>
      </dialog>
    </div>
  );
}
