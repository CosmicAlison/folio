import { useEffect, useRef, useState } from "react";
import {
  BookOpen,
  Plus,
  Search,
  ArrowUpRight,
  MessageSquare,
  ArrowRight,
  LockKeyhole,
  FileText,
  Layers,
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
import ThemeToggle from "./components/ThemeToggle";
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
      if (!response.ok) {
        const problem = await response.json().catch(() => null);
        throw new Error(
          response.status === 401
            ? "Your session expired. Please sign in again."
            : problem?.message ||
              "The assistant is unavailable. Please try again.",
        );
      }
      setAnswer(await response.json());
    } catch (err) {
      setNotice(
        err instanceof Error && !(err instanceof TypeError)
          ? err.message
          : "Could not connect to the assistant. Please try again.",
      );
    } finally {
      setBusy(false);
    }
  }
  const filtered = blog.posts.filter((p) =>
    `${p.title} ${p.contents}`.toLowerCase().includes(query.toLowerCase()),
  );
  const latest = blog.page === 1 && !query ? blog.posts[0] : undefined;
  function date(value?: string) {
    if (!value || Number.isNaN(Date.parse(value))) return "Post";
    return new Intl.DateTimeFormat(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
    }).format(new Date(value));
  }
  return (
    <div className="workspace">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <aside className="sidebar" aria-label="Sidebar">
        <a className="brand" href="/" aria-label="Folio home">
          <Layers size={25} />
          <span className="brand-word">folio</span>
        </a>
        <div
          className="sidebar-scroll"
          role="region"
          tabIndex={0}
          aria-label="Scrollable sidebar"
        >
          <div className="workspace-switch">
            <span className="workspace-symbol">
              {user?.username.slice(0, 1).toUpperCase()}
            </span>
            <div>
              <strong>Personal workspace</strong>
              <small>{user?.username}</small>
            </div>
            <LockKeyhole size={13} />
          </div>
          <span className="nav-label">Workspace</span>
          <nav aria-label="Workspace">
            <button
              aria-label="All posts"
              aria-pressed={view === "library"}
              className={view === "library" ? "nav active" : "nav"}
              onClick={() => {
                setView("library");
                setNotice("");
              }}
            >
              <BookOpen size={18} />
              <span>All posts</span>
              <span className="count">{blog.totalPosts}</span>
            </button>
            <button
              aria-label="Ask Folio AI"
              aria-pressed={view === "assistant"}
              className={view === "assistant" ? "nav active" : "nav"}
              onClick={() => {
                setView("assistant");
                setNotice("");
              }}
            >
              <MessageSquare size={18} />
              <span>Ask Folio</span>
              <ArrowUpRight size={14} className="nav-arrow" />
            </button>
          </nav>
          {blog.posts.length > 0 && (
            <div className="sidebar-posts">
              <span className="nav-label">On this page</span>
              {blog.posts.slice(0, 6).map((post) => (
                <button key={post.id} onClick={() => setReading(post)}>
                  <FileText size={14} />
                  <span>{post.title}</span>
                </button>
              ))}
            </div>
          )}
          <div className="sidebar-footer">
            <span className="nav-label">Appearance</span>
            <ThemeToggle />
            <div className="profile">
              <span className="avatar">
                {user?.username.slice(0, 1).toUpperCase()}
              </span>
              <div>
                <strong>{user?.username}</strong>
                <small>Personal account</small>
              </div>
              <button aria-label="Sign out" onClick={logout}>
                <LogOut size={17} />
              </button>
            </div>
          </div>
        </div>
      </aside>
      <main className="main" id="main">
        <header className="topbar">
          <span>
            Personal <span className="slash">/</span>{" "}
            <strong>{view === "library" ? "Library" : "Ask Folio"}</strong>
          </span>
          <span className="private-label">
            <LockKeyhole size={13} />
            Only you
          </span>
        </header>
        <div className="page">
          <div className="page-heading">
            <div>
              <h1>{view === "library" ? "Library" : "Ask Folio"}</h1>
              <p className="subtitle">
                {view === "library"
                  ? "Write, edit, and revisit your posts."
                  : "Answers from your writing, with sources attached."}
              </p>
            </div>
            <button className="primary" onClick={() => compose("new")}>
              <Plus size={17} />
              New post
            </button>
          </div>
          {view === "library" ? (
            <>
              {latest && (
                <section
                  className="overview"
                  aria-label="Latest post and library assistant"
                >
                  <article className="featured-post">
                    <div className="featured-meta">
                      <span>Recently added</span>
                      <span>{date(latest.createdAt)}</span>
                    </div>
                    <h2>{latest.title}</h2>
                    <p>{latest.contents}</p>
                    <button onClick={() => setReading(latest)}>
                      Continue reading
                      <ArrowRight size={17} />
                    </button>
                  </article>
                  <button
                    className="assistant-teaser"
                    onClick={() => setView("assistant")}
                  >
                    <span className="research-icon">
                      <MessageSquare size={24} />
                    </span>
                    <div>
                      <span className="research-label">Library assistant</span>
                      <h2>Search your posts.</h2>
                      <p>Find relevant passages across your entire library.</p>
                    </div>
                    <span className="teaser-link">
                      Ask a question
                      <ArrowUpRight size={18} />
                    </span>
                  </button>
                </section>
              )}
              <section aria-label="Posts">
                <div className="library-heading">
                  <h2>
                    All posts <span>{blog.totalPosts}</span>
                  </h2>
                  <div className="library-tools">
                    <label className="search">
                      <Search size={16} />
                      <input
                        aria-label="Search posts on this page"
                        placeholder="Search this page"
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
                        <LayoutGrid size={16} />
                      </button>
                      <button
                        aria-label="List view"
                        aria-pressed={layout === "list"}
                        onClick={() => setLayout("list")}
                      >
                        <List size={17} />
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
                  <div className="empty" role="status">
                    Loading posts…
                  </div>
                ) : filtered.length ? (
                  <div className={`posts ${layout}`}>
                    {filtered.map((post) => (
                      <article className="post" key={post.id}>
                        <div className="post-top">
                          <FileText size={15} />
                          <span>{date(post.createdAt)}</span>
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
                              <Pencil size={15} />
                            </button>
                            <button
                              aria-label={`Delete ${post.title}`}
                              onClick={() => {
                                setNotice("");
                                setRemoving(post);
                              }}
                            >
                              <Trash2 size={15} />
                            </button>
                            <button
                              aria-label={`Read ${post.title}`}
                              onClick={() => setReading(post)}
                            >
                              <ArrowUpRight size={18} />
                            </button>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="empty">
                    <span className="empty-icon">
                      <FileText size={28} />
                    </span>
                    <h3>
                      {query
                        ? "No matching posts on this page"
                        : "Your library is empty"}
                    </h3>
                    <p>
                      {query
                        ? "Try another search or change pages."
                        : "Create a post to start your collection."}
                    </p>
                    {!query && (
                      <button
                        className="primary"
                        onClick={() => compose("new")}
                      >
                        <Plus size={16} />
                        Write your first post
                      </button>
                    )}
                  </div>
                )}
                <footer className="pagination">
                  <span>
                    {blog.totalPosts} posts{" "}
                    <span className="pagination-divider">/</span> Page{" "}
                    {blog.page} of {blog.totalPages}
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
                      <ArrowRight size={14} />
                    </button>
                  </div>
                </footer>
              </section>
            </>
          ) : (
            <section className="assistant-panel">
              <div className="assistant-intro">
                <span className="assistant-mark">
                  <MessageSquare size={28} />
                </span>
                <h2>What are you looking for?</h2>
                <p>
                  Search across your posts. Folio returns relevant passages and,
                  when enabled, a generated answer. Check the sources before
                  relying on an answer.
                </p>
              </div>
              <div className="suggestions">
                {[
                  "What have I written about design?",
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
                    placeholder="Ask about your posts…"
                  />
                  <button
                    className="primary"
                    disabled={busy || !question.trim()}
                  >
                    <Send size={17} />
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
                      ? "Generated answer"
                      : "Library search"}
                  </span>
                  <p>{answer.answer}</p>
                  <h3>Sources</h3>
                  {answer.sources.length ? (
                    answer.sources.map((source, i) => (
                      <div className="source" key={`${source.postId}-${i}`}>
                        <strong>
                          [{source.postId}] {source.title}
                        </strong>
                        <p>{source.excerpt}</p>
                      </div>
                    ))
                  ) : (
                    <p>No matching sources.</p>
                  )}
                </div>
              )}
            </section>
          )}
        </div>
      </main>
      <dialog
        ref={dialog}
        aria-label={
          editor !== null
            ? "Post editor"
            : removing
              ? "Delete post confirmation"
              : "Read post"
        }
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
                {editor === "new" ? "New post" : "Editing post"}
              </span>
              <h2>{editor === "new" ? "Create a post" : "Edit post"}</h2>
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
