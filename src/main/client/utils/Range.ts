/**
 * Represents a range with a start position and an end position
 */
export default class Range {
    /**
     * Parses a string representation to create a Range.
     *
     * @param str A string representing the Range.
     */
    public static parse(str?: string | null): Range {
        if(!str) {
            return Range.EMPTY;
        }
        let m = /B(\d+)(-(\d+))?/.exec(str);
        if(m) {
            const start = parseInt(m[1]), end = parseInt(m[2] ? m[3] : m[1]);
            if(start < end) {
                return new Range(start, end);
            } else {
                return new Range(end, start);
            }
        }
        return Range.EMPTY;
    }

    /**
     * Create a Range from the start and end positions.
     *
     * @param start Starting position
     * @param end End position
     */
    public static of(start: number, end: number): Range {
        if(isNaN(start) || isNaN(end)) {
            return Range.EMPTY;
        } else if(start < end) {
            return new Range(start, end);
        } else {
            return new Range(end, start);
        }
    }

    /**
     * Returns a safe range for null and undefined.
     *
     * @param range Unsafe range instance
     */
    public static safe(range?: Range | null): Range {
        if(range === null || typeof range === 'undefined') {
            return Range.EMPTY;
        }
        return range as Range;
    }

    /**
     * Returns an empty Range.
     */
    public static empty(): Range {
        return Range.EMPTY;
    }

    /** An empty Range  */
    private static readonly EMPTY = new Range(-1, -10);

    /** Starting position (including this value) */
    public readonly start: number;

    /** End position (including this value) */
    public readonly end: number;

    /**
     * Private constructor
     *
     * @param start Starting position (including this value)
     * @param end End position (including this value)
     */
    private constructor(start: number, end: number) {
        this.start = start;
        this.end = end;
    }

    /**
     * Returns true if this Range is empty, otherwise false.
     */
    public get isEmpty(): boolean {
        return this.start > this.end;
    }

    /**
     * Returns true if this Range is not empty, otherwise false.
     */
    public get isValid(): boolean {
        return this.start <= this.end;
    }

    /**
     * Returns true if the specified value is within this range, otherwise false.
     *
     * @param value Value to test
     */
    public contains(value: number): boolean {
        return this.start <= value && value <= this.end;
    }

    /**
     * Returns a string representation of this Range.
     */
    public toString(): string {
        if(this.start === this.end) {
            return `B${this.start}`;
        } else if(this.start <= this.end) {
            return `B${this.start}-${this.end}`;
        } else {
            return '';
        }
    }
}
