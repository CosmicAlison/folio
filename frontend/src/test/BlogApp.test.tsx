import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BlogApp from "../BlogApp";
const { addPost, editPost, deletePost } = vi.hoisted(() => ({
  addPost: vi.fn(),
  editPost: vi.fn(),
  deletePost: vi.fn(),
}));
vi.mock("../context/AuthContext", () => ({
  useAuth: () => ({
    user: { username: "Writer" },
    accessToken: "test",
    logout: vi.fn(),
  }),
}));
vi.mock("../components/useBlog", () => ({
  useBlog: () => ({
    posts: [{ id: 1, title: "Design notes", contents: "Clear labels matter." }],
    page: 1,
    totalPosts: 1,
    totalPages: 1,
    isLoading: false,
    error: null,
    addPost,
    editPost,
    deletePost,
    changePage: vi.fn(),
  }),
}));
describe("Folio workspace", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.unstubAllGlobals();
  });
  it("reads and filters posts", () => {
    render(<BlogApp />);
    fireEvent.click(screen.getByRole("button", { name: "Read Design notes" }));
    expect(screen.getByRole("dialog")).toHaveTextContent(
      "Clear labels matter.",
    );
    fireEvent.click(screen.getByRole("button", { name: "Back to library" }));
    fireEvent.change(
      screen.getByRole("textbox", { name: "Search posts on this page" }),
      { target: { value: "missing" } },
    );
    expect(
      screen.getByText("No matching posts on this page"),
    ).toBeInTheDocument();
  });
  it("keeps unsaved writing after a failed request", async () => {
    addPost.mockRejectedValue(new Error("offline"));
    render(<BlogApp />);
    fireEvent.click(screen.getByRole("button", { name: "New post" }));
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "New idea" },
    });
    fireEvent.change(screen.getByLabelText("Your writing"), {
      target: { value: "Keep this text" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save post" }));
    await screen.findByText(/Your writing is still here/);
    expect(screen.getByLabelText("Your writing")).toHaveValue("Keep this text");
  });
  it("waits for successful saves before closing", async () => {
    addPost.mockResolvedValue(undefined);
    render(<BlogApp />);
    fireEvent.click(screen.getByRole("button", { name: "New post" }));
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "New idea" },
    });
    fireEvent.change(screen.getByLabelText("Your writing"), {
      target: { value: "Body" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save post" }));
    await waitFor(() =>
      expect(addPost).toHaveBeenCalledWith({
        title: "New idea",
        contents: "Body",
      }),
    );
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
  });
  it("requires confirmation before deleting", async () => {
    deletePost.mockResolvedValue(undefined);
    render(<BlogApp />);
    fireEvent.click(
      screen.getByRole("button", { name: "Delete Design notes" }),
    );
    expect(deletePost).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Delete post" }));
    await waitFor(() => expect(deletePost).toHaveBeenCalledWith(1));
  });
  it("shows cited assistant answers", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          answer: "Labels matter [1]",
          mode: "rag",
          sources: [
            {
              postId: 1,
              title: "Design notes",
              excerpt: "Clear labels matter.",
            },
          ],
        }),
      }),
    );
    render(<BlogApp />);
    fireEvent.click(screen.getByRole("button", { name: "Ask Folio AI" }));
    fireEvent.change(screen.getByLabelText("Your question"), {
      target: { value: "What about design?" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Ask Folio" }));
    expect(await screen.findByText("Labels matter [1]")).toBeInTheDocument();
    expect(screen.getByText("[1] Design notes")).toBeInTheDocument();
  });
  it("shows the safe server error when hosted AI cannot answer", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 503,
        json: async () => ({
          message: "The AI provider account has no available credits.",
        }),
      }),
    );
    render(<BlogApp />);
    fireEvent.click(screen.getByRole("button", { name: "Ask Folio AI" }));
    fireEvent.change(screen.getByLabelText("Your question"), {
      target: { value: "design" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Ask Folio" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The AI provider account has no available credits.",
    );
    expect(screen.queryByText("Sources")).not.toBeInTheDocument();
  });
});
