Tag List – Format and Guidelines
--------------------------------

This folder contains the file `tags.txt`, which defines a list of standardized tags used for classifying content related to digital twins, industrial applications, and product metadata.

Each line in `tags.txt` must follow the format:

    TagName:Category

Example:

    Digital Twin:Technology
    Predictive Maintenance:Use Case

Guidelines
----------

1. Each tag must belong to exactly one category.
2. A tag name may only appear once in the file. Duplicate entries with different categories are not allowed.
3. Tag names are case-insensitive. For example, "AI" and "ai" are treated as the same tag.
4. Lines without a valid format (missing colon, empty values) will be ignored or may cause errors during initialization.

Categories in Use
-----------------

The following categories are currently supported:

- Technology
- Use Case
- Concept
- Data
- Infrastructure
- Standard
- Integration
- Process
- System
- Hardware
- Sustainability
- Quality

Purpose
-------

This tagging system helps to organize, classify, and filter digital twin-related data across the platform. Tags are used in metadata, search filters, and other classification features.

Please ensure consistency and accuracy when editing or extending the tag list.