/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

/**
 * HttpUri
 *
 * @author Otstar Lin
 * @date 2020/10/27 下午 3:48
 */
public class HttpUri {

    private enum State {
        /**
         * 开始
         */
        START,
        /**
         * Host 和 Path
         */
        HOST_OR_PATH,
        /**
         * Scheme 和 Path
         */
        SCHEME_OR_PATH,
        /**
         * 域名或 IPv4 地址
         */
        HOST,
        /**
         * IPv6 地址
         */
        IPV6,
        /**
         * 端口
         */
        PORT,
        /**
         * 路径
         */
        PATH,
        /**
         * 参数
         */
        PARAM,
        /**
         * Query 参数
         */
        QUERY,
        /**
         * 片段
         */
        FRAGMENT,
        /**
         * 星号
         */
        ASTERISK,
    }

    private String scheme;
    private String user;
    private String host;
    private int port;
    private String path;
    private String param;
    private String query;
    private String fragment;
    private String uri;
    private String decodedPath;

    public HttpUri() {}

    public HttpUri(final String uri) {
        this.uri = uri;
        this.parse(uri);
    }

    public HttpUri(final HttpUri uri) {
        scheme = uri.getScheme();
        user = uri.getUser();
        host = uri.getHost();
        port = uri.getPort();
        path = uri.getPath();
        param = uri.getParam();
        query = uri.getQuery();
        fragment = uri.getFragment();
        this.uri = null;
        decodedPath = uri.getDecodedPath();
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setParam(final String param) {
        this.param = param;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public void setFragment(final String fragment) {
        this.fragment = fragment;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public void setDecodedPath(final String decodedPath) {
        this.decodedPath = decodedPath;
    }

    public String getScheme() {
        return scheme;
    }

    public String getUser() {
        return user;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getParam() {
        return param;
    }

    public String getQuery() {
        return query;
    }

    public String getFragment() {
        return fragment;
    }

    public String getUri() {
        return uri;
    }

    public boolean isAbsolute() {
        return scheme != null && !scheme.isEmpty();
    }

    public String getDecodedPath() {
        return decodedPath;
    }

    public String getAuthority() {
        if (port > 0) {
            return host + ":" + port;
        }
        return host;
    }

    public boolean hasAuthority() {
        return host != null;
    }

    public String asString() {
        if (uri == null) {
            final StringBuilder out = new StringBuilder();
            if (scheme != null) {
                out.append(scheme).append(':');
            }
            if (host != null) {
                out.append("//");
                if (user != null) {
                    out.append(user).append('@');
                }
                out.append(host);
            }
            if (port > 0) {
                out.append(':').append(port);
            }
            if (path != null) {
                out.append(path);
            }
            if (query != null) {
                out.append('?').append(query);
            }
            if (fragment != null) {
                out.append('#').append(fragment);
            }
            if (out.length() > 0) {
                uri = out.toString();
            } else {
                uri = "";
            }
        }
        return uri;
    }

    @Override
    public String toString() {
        return uri;
    }

    private void parse(final String uri) {
        State state = State.START;
        boolean encoded = false;
        final int end = uri.length();
        int mark = 0;
        int pathMark = 0;
        char last = '/';
        for (int i = 0; i < end; i++) {
            char c = uri.charAt(i);

            switch (state) {
                case START: {
                    switch (c) {
                        case '/':
                            mark = i;
                            state = State.HOST_OR_PATH;
                            break;
                        case ';':
                            mark = i + 1;
                            state = State.PARAM;
                            break;
                        case '?':
                            // assume empty path (if seen at start)
                            path = "";
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        case '#':
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        case '*':
                            path = "*";
                            state = State.ASTERISK;
                            break;
                        case '.':
                            pathMark = i;
                            state = State.PATH;
                            encoded = true;
                            break;
                        default:
                            mark = i;
                            if (scheme == null) {
                                state = State.SCHEME_OR_PATH;
                            } else {
                                pathMark = i;
                                state = State.PATH;
                            }
                            break;
                    }

                    continue;
                }
                case SCHEME_OR_PATH: {
                    switch (c) {
                        case ':':
                            // must have been a scheme
                            scheme = uri.substring(mark, i);
                            // Start again with scheme set
                            state = State.START;
                            break;
                        case '/':
                            // must have been in a path and still are
                            state = State.PATH;
                            break;
                        case ';':
                            // must have been in a path
                            mark = i + 1;
                            state = State.PARAM;
                            break;
                        case '?':
                            // must have been in a path
                            path = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        case '%':
                            // must have be in an encoded path
                            encoded = true;
                            state = State.PATH;
                            break;
                        case '#':
                            // must have been in a path
                            path = uri.substring(mark, i);
                            state = State.FRAGMENT;
                            break;
                        default:
                            break;
                    }
                    continue;
                }
                case HOST_OR_PATH: {
                    switch (c) {
                        case '/':
                            host = "";
                            mark = i + 1;
                            state = State.HOST;
                            break;
                        case '@':
                        case ';':
                        case '?':
                        case '#':
                            // was a path, look again
                            i--;
                            pathMark = mark;
                            state = State.PATH;
                            break;
                        case '.':
                            // it is a path
                            encoded = true;
                            pathMark = mark;
                            state = State.PATH;
                            break;
                        default:
                            // it is a path
                            pathMark = mark;
                            state = State.PATH;
                    }
                    continue;
                }
                case HOST: {
                    switch (c) {
                        case '/':
                            host = uri.substring(mark, i);
                            pathMark = mark = i;
                            state = State.PATH;
                            break;
                        case ':':
                            if (i > mark) {
                                host = uri.substring(mark, i);
                            }
                            mark = i + 1;
                            state = State.PORT;
                            break;
                        case '@':
                            if (user != null) {
                                throw new IllegalArgumentException(
                                    "Bad authority");
                            }
                            user = uri.substring(mark, i);
                            mark = i + 1;
                            break;
                        case '[':
                            state = State.IPV6;
                            break;
                        default:
                            break;
                    }
                    break;
                }
                case IPV6: {
                    switch (c) {
                        case '/':
                            throw new IllegalArgumentException(
                                "No closing ']' for ipv6 in " + uri);
                        case ']':
                            c = uri.charAt(++i);
                            host = uri.substring(mark, i);
                            if (c == ':') {
                                mark = i + 1;
                                state = State.PORT;
                            } else {
                                pathMark = mark = i;
                                state = State.PATH;
                            }
                            break;
                        default:
                            break;
                    }

                    break;
                }
                case PORT: {
                    if (c == '@') {
                        if (user != null) {
                            throw new IllegalArgumentException("Bad authority");
                        }
                        // It wasn't a port, but a password!
                        user = host + ":" + uri.substring(mark, i);
                        mark = i + 1;
                        state = State.HOST;
                    } else if (c == '/') {
                        port = Integer.parseInt(uri.substring(mark, i));
                        pathMark = mark = i;
                        state = State.PATH;
                    }
                    break;
                }
                case PATH: {
                    switch (c) {
                        case ';':
                            mark = i + 1;
                            state = State.PARAM;
                            break;
                        case '?':
                            path = uri.substring(pathMark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        case '#':
                            path = uri.substring(pathMark, i);
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        case '%':
                            encoded = true;
                            break;
                        case '.':
                            if ('/' == last) {
                                encoded = true;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                }
                case PARAM: {
                    switch (c) {
                        case '?':
                            path = uri.substring(pathMark, i);
                            param = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        case '#':
                            path = uri.substring(pathMark, i);
                            param = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        case '/':
                            encoded = true;
                            // ignore internal params
                            state = State.PATH;
                            break;
                        case ';':
                            // multiple parameters
                            mark = i + 1;
                            break;
                        default:
                            break;
                    }
                    break;
                }
                case QUERY: {
                    if (c == '#') {
                        query = uri.substring(mark, i);
                        mark = i + 1;
                        state = State.FRAGMENT;
                    }
                    break;
                }
                case ASTERISK: {
                    throw new IllegalArgumentException("Bad character '*'");
                }
                case FRAGMENT: {
                    i = end;
                    break;
                }
                default:
                    throw new IllegalStateException(state.toString());
            }
            last = c;
        }

        switch (state) {
            case START:
            case ASTERISK:
                break;
            case SCHEME_OR_PATH:
                path = uri.substring(mark, end);
                break;
            case HOST_OR_PATH:
                path = uri.substring(mark, end);
                break;
            case HOST:
                if (end > mark) {
                    host = uri.substring(mark, end);
                }
                break;
            case IPV6:
                throw new IllegalArgumentException(
                    "No closing ']' for ipv6 in " + uri);
            case PORT:
                port = Integer.parseInt(uri.substring(mark, end));
                break;
            case PARAM:
                path = uri.substring(pathMark, end);
                param = uri.substring(mark, end);
                break;
            case PATH:
                path = uri.substring(pathMark, end);
                break;
            case QUERY:
                query = uri.substring(mark, end);
                break;
            case FRAGMENT:
                fragment = uri.substring(mark, end);
                break;
            default:
                throw new IllegalStateException(state.toString());
        }

        if (!encoded) {
            if (param == null) {
                decodedPath = path;
            } else {
                decodedPath = path
                    .substring(0, path.length() - param.length() - 1);
            }
        }
    }
}
