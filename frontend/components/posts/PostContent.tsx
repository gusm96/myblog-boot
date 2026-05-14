import parse, { type DOMNode, Element } from "html-react-parser";
import Image from "next/image";

interface PostContentProps {
  content: string;
}

export default function PostContent({ content }: PostContentProps) {
  return (
    <div className="post-content">
      {parse(content, {
        replace: (node: DOMNode) => {
          if (node instanceof Element && node.name === "img") {
            const { src, alt = "" } = node.attribs;
            if (!src) return;
            return (
              <Image
                src={src}
                alt={alt}
                width={1200}
                height={675}
                sizes="(max-width: 768px) 100vw, 768px"
                style={{ width: "100%", height: "auto" }}
              />
            );
          }
        },
      })}
    </div>
  );
}
